/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.runtime.vm;

import static org.tron.common.runtime.utils.MUtil.convertToTronAddress;
import static org.tron.common.runtime.vm.program.Program.VALIDATE_FOR_SMART_CONTRACT_FAILURE;
import static org.tron.common.utils.BIUtil.addSafely;
import static org.tron.common.utils.BIUtil.isLessThan;
import static org.tron.common.utils.BIUtil.isZero;
import static org.tron.common.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tron.common.utils.ByteUtil.bytesToBigInteger;
import static org.tron.common.utils.ByteUtil.numberOfLeadingZeros;
import static org.tron.common.utils.ByteUtil.parseBytes;
import static org.tron.common.utils.ByteUtil.parseWord;
import static org.tron.common.utils.ByteUtil.stripLeadingZeroes;

import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.zksnark.BN128;
import org.tron.common.crypto.zksnark.BN128Fp;
import org.tron.common.crypto.zksnark.BN128G1;
import org.tron.common.crypto.zksnark.BN128G2;
import org.tron.common.crypto.zksnark.Fp;
import org.tron.common.crypto.zksnark.PairingCheck;
import org.tron.common.runtime.utils.MUtil;
import org.tron.common.runtime.vm.program.Program;
import org.tron.common.runtime.vm.program.Program.BytecodeExecutionException;
import org.tron.common.runtime.vm.program.Program.PrecompiledContractException;
import org.tron.common.runtime.vm.program.ProgramResult;
import org.tron.common.storage.Deposit;
import org.tron.common.utils.BIUtil;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.actuator.ProposalApproveActuator;
import org.tron.core.actuator.ProposalDeleteActuator;
import org.tron.core.actuator.VoteWitnessActuator;
import org.tron.core.actuator.WithdrawBalanceActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Contract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.ProposalApproveContract;
import org.tron.protos.Contract.ProposalDeleteContract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

/**
 * @author Roman Mandeleil
 * @since 09.01.2015
 */

@Slf4j(topic = "VM")
public class PrecompiledContracts {

  private static final ECRecover ecRecover = new ECRecover();
  private static final Sha256 sha256 = new Sha256();
  private static final Ripempd160 ripempd160 = new Ripempd160();
  private static final Identity identity = new Identity();
  private static final ModExp modExp = new ModExp();
  private static final BN128Addition altBN128Add = new BN128Addition();
  private static final BN128Multiplication altBN128Mul = new BN128Multiplication();
  private static final BN128Pairing altBN128Pairing = new BN128Pairing();
  private static final Mine mine = new Mine();
  private static final MineToken mineToken = new MineToken();
  private static final MultiValidateSign multiValidateSign = new MultiValidateSign();

  private static final ECKey addressCheckECKey = new ECKey();
  private static final String addressCheckECKeyAddress = Wallet
      .encode58Check(addressCheckECKey.getAddress());

  private static final DataWord mineAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010000");
  private static final DataWord mineTokenAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000010001");
  private static final DataWord ecRecoverAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000001");
  private static final DataWord sha256Addr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000002");
  private static final DataWord ripempd160Addr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000003");
  private static final DataWord identityAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000004");
  private static final DataWord modExpAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000005");
  private static final DataWord altBN128AddAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000006");
  private static final DataWord altBN128MulAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000007");
  private static final DataWord altBN128PairingAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000008");
  private static final DataWord multiValidateSignAddr = new DataWord(
      "0000000000000000000000000000000000000000000000000000000000000009");

  public static PrecompiledContract getContractForAddress(DataWord address) {

    if (address == null) {
      return identity;
    }
    if (address.equals(mineAddr)) {
      return mine;
    }
    if (address.equals(mineTokenAddr)) {
      return mineToken;
    }
    if (address.equals(ecRecoverAddr)) {
      return ecRecover;
    }
    if (address.equals(sha256Addr)) {
      return sha256;
    }
    if (address.equals(ripempd160Addr)) {
      return ripempd160;
    }
    if (address.equals(identityAddr)) {
      return identity;
    }
    // Byzantium precompiles
    if (address.equals(modExpAddr)) {
      return modExp;
    }
    if (address.equals(altBN128AddAddr)) {
      return altBN128Add;
    }
    if (address.equals(altBN128MulAddr)) {
      return altBN128Mul;
    }
    if (address.equals(altBN128PairingAddr)) {
      return altBN128Pairing;
    }
    if (address.equals(multiValidateSignAddr)) {
      return multiValidateSign;
    }
    return null;
  }

  private static byte[] encodeRes(byte[] w1, byte[] w2) {

    byte[] res = new byte[64];

    w1 = stripLeadingZeroes(w1);
    w2 = stripLeadingZeroes(w2);

    System.arraycopy(w1, 0, res, 32 - w1.length, w1.length);
    System.arraycopy(w2, 0, res, 64 - w2.length, w2.length);

    return res;
  }

  public static abstract class PrecompiledContract {

    public abstract long getEnergyForData(byte[] data);

    public abstract Pair<Boolean, byte[]> execute(byte[] data);

    private byte[] callerAddress;

    public void setCallerAddress(byte[] callerAddress) {
      this.callerAddress = callerAddress.clone();
    }

    public void setDeposit(Deposit deposit) {
      this.deposit = deposit;
    }

    public void setResult(ProgramResult result) {
      this.result = result;
    }

    private Deposit deposit;

    private ProgramResult result;

    public byte[] getCallerAddress() {
      return callerAddress.clone();
    }

    public Deposit getDeposit() {
      return deposit;
    }

    public ProgramResult getResult() {
      return result;
    }



    @Getter
    @Setter
    private boolean isStaticCall;

    @Getter
    @Setter
    private long vmShouldEndInUs;


    public long getCPUTimeLeftInUs(){
      long vmNowInUs = System.nanoTime() / 1000;
      long left = getVmShouldEndInUs() - vmNowInUs;
      if(left <= 0) {
        throw Program.Exception.notEnoughTime("call");
      } else {
        return  left;
      }
    }
  }

  public static class MineToken extends  PrecompiledContract{
    @Override
    public long getEnergyForData(byte[] data) {
      return 0;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      if (isStaticCall()) {
        return Pair.of(true, new DataWord(0).getData());
      }
      if (data == null || data.length != 5 * DataWord.WORD_SIZE) {
        return Pair.of(false, new DataWord(0).getData());
      }
      List<byte[]> gatewayList = this.getDeposit().getSideChainGateWayList();
      boolean match = false;
      for (byte[] gateway: gatewayList) {
        if (ByteUtil.equals(gateway, this.getCallerAddress())) {
          match = true;
          break;
        }
      }
      if (!match) {
        logger.error("[mineToken method]caller must be gateway, caller: %s", Wallet.encode58Check(this.getCallerAddress()));
        throw new PrecompiledContractException("[mineToken method]caller must be gateway, caller: %s", Wallet.encode58Check(this.getCallerAddress()));
      }

      long amount = new DataWord(Arrays.copyOf(data, 32)).sValue().longValueExact();

      byte[] tokenId = new DataWord(Arrays.copyOfRange(data,32,64)).getData();

      byte[] tokenIdWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenId);
      byte[] tokenIdLongBytes = String.valueOf(Long.parseLong(Hex.toHexString(tokenIdWithoutLeadingZero),16)).getBytes();

      byte[] tokenName = new DataWord(Arrays.copyOfRange(data, 64,96)).getData();

      byte[] symbol = new DataWord(Arrays.copyOfRange(data, 96, 128)).getData();

      int decimals = new DataWord(Arrays.copyOfRange(data, 128, 160)).sValue().intValueExact();

      validateMineTokenProcess(amount, tokenIdLongBytes, tokenName, symbol, decimals);

      getDeposit().addTokenBalance(this.getCallerAddress(), tokenIdLongBytes, amount);

      return Pair.of(true, EMPTY_BYTE_ARRAY);
    }

    private void validateMineTokenProcess(long amount, byte[] tokenId, byte[] tokenName, byte[] symbol, int decimals) {

      byte[] tokenNameWithoutLeadingZero = ByteUtil.stripLeadingZeroes(tokenName);
      byte[] tokenSymbol = ByteUtil.stripLeadingZeroes(symbol);
      long tokenIdLong = new DataWord(tokenId).sValue().longValueExact();
      if (amount < 0) {
        throw new PrecompiledContractException("[mineToken method]amount must be greater than 0: %s", amount);
      }
      if (tokenIdLong <= VMConstant.MIN_TOKEN_ID) {
        throw new BytecodeExecutionException(
            VALIDATE_FOR_SMART_CONTRACT_FAILURE + ", not valid token id");
      }

      if (getDeposit().getAssetIssue(tokenId) == null) {
        AssetIssueContract.Builder assetBuilder = AssetIssueContract.newBuilder();
        assetBuilder
            .setId(new String(tokenId))
            .setName(ByteString.copyFrom(tokenNameWithoutLeadingZero))
            .setAbbr(ByteString.copyFrom(tokenSymbol))
            .setPrecision(decimals);
        AssetIssueCapsule assetIssueCapsuleV2 = new AssetIssueCapsule(assetBuilder.build());
        getDeposit().putAssetIssue(assetIssueCapsuleV2.createDbV2Key(),assetIssueCapsuleV2);
      }

      AccountCapsule callerAccount = getDeposit().getAccount(this.getCallerAddress());
      if (callerAccount != null) {
        Long assetBalance = callerAccount.getAssetMapV2()
            .get(ByteArray.toStr(tokenId));
        if (assetBalance != null) {
          try {
            assetBalance = Math.addExact(assetBalance, amount); //check if overflow
          } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            throw new BytecodeExecutionException(e.getMessage());
          }
        }
      }
    }
  }

  public static class Mine extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 0;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      List<byte[]> gatewayList = this.getDeposit().getSideChainGateWayList();
      boolean match = false;
      for (byte[] gateway: gatewayList) {
        if (ByteUtil.equals(gateway, this.getCallerAddress())) {
          match = true;
          break;
        }
      }
      if (!match) {
        logger.error("[mine method]caller must be gateway, caller: %s", Wallet.encode58Check(this.getCallerAddress()));
        throw new PrecompiledContractException("[mine method]caller must be gateway, caller: %s", Wallet.encode58Check(this.getCallerAddress()));
      }

      long amount = new DataWord(Arrays.copyOf(data, 32)).sValue().longValueExact();
      if (amount <= 0) {
        throw new PrecompiledContractException("[mine method]amount must be greater than 0: %s", amount);
      }
      this.getDeposit().addBalance(this.getCallerAddress(), amount);

      return Pair.of(true, EMPTY_BYTE_ARRAY);
    }
  }

  public static class Identity extends PrecompiledContract {

    public Identity() {
    }

    @Override
    public long getEnergyForData(byte[] data) {

      // energy charge for the execution:
      // minimum 1 and additional 1 for each 32 bytes word (round  up)
      if (data == null) {
        return 15;
      }
      return 15L + (data.length + 31) / 32 * 3;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      return Pair.of(true, data);
    }
  }

  public static class Sha256 extends PrecompiledContract {


    @Override
    public long getEnergyForData(byte[] data) {

      // energy charge for the execution:
      // minimum 50 and additional 50 for each 32 bytes word (round  up)
      if (data == null) {
        return 60;
      }
      return 60L + (data.length + 31) / 32 * 12;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        return Pair.of(true, Sha256Hash.hash(EMPTY_BYTE_ARRAY));
      }
      return Pair.of(true, Sha256Hash.hash(data));
    }
  }


  public static class Ripempd160 extends PrecompiledContract {


    @Override
    public long getEnergyForData(byte[] data) {

      // TODO #POC9 Replace magic numbers with constants
      // energy charge for the execution:
      // minimum 50 and additional 50 for each 32 bytes word (round  up)
      if (data == null) {
        return 600;
      }
      return 600L + (data.length + 31) / 32 * 120;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      byte[] target = new byte[20];
      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }
      byte[] orig = Sha256Hash.hash(data);
      System.arraycopy(orig, 0, target, 0, 20);
      return Pair.of(true, Sha256Hash.hash(target));
    }
  }


  public static class ECRecover extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 3000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      byte[] h = new byte[32];
      byte[] v = new byte[32];
      byte[] r = new byte[32];
      byte[] s = new byte[32];

      DataWord out = null;

      try {
        System.arraycopy(data, 0, h, 0, 32);
        System.arraycopy(data, 32, v, 0, 32);
        System.arraycopy(data, 64, r, 0, 32);

        int sLength = data.length < 128 ? data.length - 96 : 32;
        System.arraycopy(data, 96, s, 0, sLength);

        ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v[31]);
        if (validateV(v) && signature.validateComponents()) {
          out = new DataWord(ECKey.signatureToAddress(h, signature));
        }
      } catch (Throwable any) {
      }

      if (out == null) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      } else {
        return Pair.of(true, out.getData());
      }
    }

    private static boolean validateV(byte[] v) {
      for (int i = 0; i < v.length - 1; i++) {
        if (v[i] != 0) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Computes modular exponentiation on big numbers
   *
   * format of data[] array: [length_of_BASE] [length_of_EXPONENT] [length_of_MODULUS] [BASE]
   * [EXPONENT] [MODULUS] where every length is a 32-byte left-padded integer representing the
   * number of bytes. Call data is assumed to be infinitely right-padded with zero bytes.
   *
   * Returns an output as a byte array with the same length as the modulus
   */
  public static class ModExp extends PrecompiledContract {

    private static final BigInteger GQUAD_DIVISOR = BigInteger.valueOf(20);

    private static final int ARGS_OFFSET = 32 * 3; // addresses length part

    @Override
    public long getEnergyForData(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      int baseLen = parseLen(data, 0);
      int expLen = parseLen(data, 1);
      int modLen = parseLen(data, 2);

      byte[] expHighBytes = parseBytes(data, addSafely(ARGS_OFFSET, baseLen), Math.min(expLen, 32));

      long multComplexity = getMultComplexity(Math.max(baseLen, modLen));
      long adjExpLen = getAdjustedExponentLength(expHighBytes, expLen);

      // use big numbers to stay safe in case of overflow
      BigInteger energy = BigInteger.valueOf(multComplexity)
          .multiply(BigInteger.valueOf(Math.max(adjExpLen, 1)))
          .divide(GQUAD_DIVISOR);

      return isLessThan(energy, BigInteger.valueOf(Long.MAX_VALUE)) ? energy.longValueExact()
          : Long.MAX_VALUE;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      }

      int baseLen = parseLen(data, 0);
      int expLen = parseLen(data, 1);
      int modLen = parseLen(data, 2);

      BigInteger base = parseArg(data, ARGS_OFFSET, baseLen);
      BigInteger exp = parseArg(data, addSafely(ARGS_OFFSET, baseLen), expLen);
      BigInteger mod = parseArg(data, addSafely(addSafely(ARGS_OFFSET, baseLen), expLen), modLen);

      // check if modulus is zero
      if (isZero(mod)) {
        return Pair.of(true, EMPTY_BYTE_ARRAY);
      }

      byte[] res = stripLeadingZeroes(base.modPow(exp, mod).toByteArray());

      // adjust result to the same length as the modulus has
      if (res.length < modLen) {

        byte[] adjRes = new byte[modLen];
        System.arraycopy(res, 0, adjRes, modLen - res.length, res.length);

        return Pair.of(true, adjRes);

      } else {
        return Pair.of(true, res);
      }
    }

    private long getMultComplexity(long x) {

      long x2 = x * x;

      if (x <= 64) {
        return x2;
      }
      if (x <= 1024) {
        return x2 / 4 + 96 * x - 3072;
      }

      return x2 / 16 + 480 * x - 199680;
    }

    private long getAdjustedExponentLength(byte[] expHighBytes, long expLen) {

      int leadingZeros = numberOfLeadingZeros(expHighBytes);
      int highestBit = 8 * expHighBytes.length - leadingZeros;

      // set index basement to zero
      if (highestBit > 0) {
        highestBit--;
      }

      if (expLen <= 32) {
        return highestBit;
      } else {
        return 8 * (expLen - 32) + highestBit;
      }
    }

    private int parseLen(byte[] data, int idx) {
      byte[] bytes = parseBytes(data, 32 * idx, 32);
      return new DataWord(bytes).intValueSafe();
    }

    private BigInteger parseArg(byte[] data, int offset, int len) {
      byte[] bytes = parseBytes(data, offset, len);
      return bytesToBigInteger(bytes);
    }
  }

  /**
   * Computes point addition on Barreto–Naehrig curve. See {@link BN128Fp} for details<br/> <br/>
   *
   * input data[]:<br/> two points encoded as (x, y), where x and y are 32-byte left-padded
   * integers,<br/> if input is shorter than expected, it's assumed to be right-padded with zero
   * bytes<br/> <br/>
   *
   * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
   * integers<br/>
   */
  public static class BN128Addition extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 500;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      byte[] x1 = parseWord(data, 0);
      byte[] y1 = parseWord(data, 1);

      byte[] x2 = parseWord(data, 2);
      byte[] y2 = parseWord(data, 3);

      BN128<Fp> p1 = BN128Fp.create(x1, y1);
      if (p1 == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      BN128<Fp> p2 = BN128Fp.create(x2, y2);
      if (p2 == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      BN128<Fp> res = p1.add(p2).toEthNotation();

      return Pair.of(true, encodeRes(res.x().bytes(), res.y().bytes()));
    }
  }

  /**
   * Computes multiplication of scalar value on a point belonging to Barreto–Naehrig curve. See
   * {@link BN128Fp} for details<br/> <br/>
   *
   * input data[]:<br/> point encoded as (x, y) is followed by scalar s, where x, y and s are
   * 32-byte left-padded integers,<br/> if input is shorter than expected, it's assumed to be
   * right-padded with zero bytes<br/> <br/>
   *
   * output:<br/> resulting point (x', y'), where x and y encoded as 32-byte left-padded
   * integers<br/>
   */
  public static class BN128Multiplication extends PrecompiledContract {

    @Override
    public long getEnergyForData(byte[] data) {
      return 40000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      byte[] x = parseWord(data, 0);
      byte[] y = parseWord(data, 1);

      byte[] s = parseWord(data, 2);

      BN128<Fp> p = BN128Fp.create(x, y);
      if (p == null) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      BN128<Fp> res = p.mul(BIUtil.toBI(s)).toEthNotation();

      return Pair.of(true, encodeRes(res.x().bytes(), res.y().bytes()));
    }
  }

  /**
   * Computes pairing check. <br/> See {@link PairingCheck} for details.<br/> <br/>
   *
   * Input data[]: <br/> an array of points (a1, b1, ... , ak, bk), <br/> where "ai" is a point of
   * {@link BN128Fp} curve and encoded as two 32-byte left-padded integers (x; y) <br/> "bi" is a
   * point of {@link BN128G2} curve and encoded as four 32-byte left-padded integers {@code (ai + b;
   * ci + d)}, each coordinate of the point is a big-endian {@link } number, so {@code b} precedes
   * {@code a} in the encoding: {@code (b, a; d, c)} <br/> thus each pair (ai, bi) has 192 bytes
   * length, if 192 is not a multiple of {@code data.length} then execution fails <br/> the number
   * of pairs is derived from input length by dividing it by 192 (the length of a pair) <br/> <br/>
   *
   * output: <br/> pairing product which is either 0 or 1, encoded as 32-byte left-padded integer
   * <br/>
   */
  public static class BN128Pairing extends PrecompiledContract {

    private static final int PAIR_SIZE = 192;

    @Override
    public long getEnergyForData(byte[] data) {

      if (data == null) {
        return 100000;
      }

      return 80000L * (data.length / PAIR_SIZE) + 100000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {

      if (data == null) {
        data = EMPTY_BYTE_ARRAY;
      }

      // fail if input len is not a multiple of PAIR_SIZE
      if (data.length % PAIR_SIZE > 0) {
        return Pair.of(false, EMPTY_BYTE_ARRAY);
      }

      PairingCheck check = PairingCheck.create();

      // iterating over all pairs
      for (int offset = 0; offset < data.length; offset += PAIR_SIZE) {

        Pair<BN128G1, BN128G2> pair = decodePair(data, offset);

        // fail if decoding has failed
        if (pair == null) {
          return Pair.of(false, EMPTY_BYTE_ARRAY);
        }

        check.addPair(pair.getLeft(), pair.getRight());
      }

      check.run();
      int result = check.result();

      return Pair.of(true, new DataWord(result).getData());
    }

    private Pair<BN128G1, BN128G2> decodePair(byte[] in, int offset) {

      byte[] x = parseWord(in, offset, 0);
      byte[] y = parseWord(in, offset, 1);

      BN128G1 p1 = BN128G1.create(x, y);

      // fail if point is invalid
      if (p1 == null) {
        return null;
      }

      // (b, a)
      byte[] b = parseWord(in, offset, 2);
      byte[] a = parseWord(in, offset, 3);

      // (d, c)
      byte[] d = parseWord(in, offset, 4);
      byte[] c = parseWord(in, offset, 5);

      BN128G2 p2 = BN128G2.create(a, b, c, d);

      // fail if point is invalid
      if (p2 == null) {
        return null;
      }

      return Pair.of(p1, p2);
    }
  }


  public static class MultiValidateSign extends PrecompiledContract {
    private static final ExecutorService workers;
    private static final int ENGERYPERSIGN = 1500;
    private static final byte[] ZEROADDR = MUtil.allZero32TronAddress();
    private static final byte[] EMPTYADDR = new byte[32];

    static {
      workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    }

    @Data
    @AllArgsConstructor
    private static class ValidateSignTask implements Callable<ValidateSignResult> {

      private CountDownLatch countDownLatch;
      private byte[] hash;
      private byte[] signature;
      private byte[] address;
      private int nonce;

      @Override
      public ValidateSignResult call() {
        try {
          return new ValidateSignResult(validSign(this.signature, this.hash, this.address), nonce);
        } finally {
          countDownLatch.countDown();
        }
      }
    }

    @Data
    @AllArgsConstructor
    private static class ValidateSignResult {

      private Boolean res;
      private int nonce;
    }

    @Override
    public long getEnergyForData(byte[] data) {
      int cnt = (data.length / DataWord.WORD_SIZE - 5) / 6;
      // one sign 1500, half of ecrecover
      return (long) (cnt * ENGERYPERSIGN);
    }

    @Override
    public Pair<Boolean, byte[]> execute(byte[] data) {
      try {
        return doExecute(data);
      } catch (Throwable t) {
        return Pair.of(true, new byte[DataWord.WORD_SIZE]);
      }
    }

    private Pair<Boolean, byte[]> doExecute(byte[] data)
        throws InterruptedException, ExecutionException {
      DataWord[] words = DataWord.parseArray(data);
      byte[] hash = words[0].getData();
      byte[][] signatures = extractBytesArray(
          words, words[1].intValueSafe() / DataWord.WORD_SIZE, data);
      byte[][] addresses = extractBytes32Array(
          words, words[2].intValueSafe() / DataWord.WORD_SIZE);
      int cnt = signatures.length;
      if (cnt == 0 || signatures.length != addresses.length) {
        return Pair.of(true, new byte[DataWord.WORD_SIZE]);
      }
      int min = Math.min(cnt, DataWord.WORD_SIZE);
      byte[] res = new byte[DataWord.WORD_SIZE];
      if (isStaticCall()) {
        //for static call not use thread pool to avoid potential effect
        for (int i = 0; i < min; i++) {
          if (validSign(signatures[i], hash, addresses[i])) {
            res[i] = 1;
          }
        }
      } else {
        // add check
        CountDownLatch countDownLatch = new CountDownLatch(min);
        List<Future<ValidateSignResult>> futures = new ArrayList<>(min);

        for (int i = 0; i < min; i++) {
          Future<ValidateSignResult> future = workers
              .submit(new ValidateSignTask(countDownLatch, hash, signatures[i], addresses[i], i));
          futures.add(future);
        }

        countDownLatch.await(getCPUTimeLeftInUs() * 1000, TimeUnit.NANOSECONDS);

        for (Future<ValidateSignResult> future : futures) {
          if (future.get() == null) {
            logger.info("MultiValidateSign timeout");
            throw Program.Exception.notEnoughTime("call MultiValidateSign precompile method");
          }
          if (future.get().getRes()) {
            res[future.get().getNonce()] = 1;
          }
        }
      }
      return Pair.of(true, res);
    }

    private static boolean validSign(byte[] sign, byte[] hash, byte[] address) {
      byte v;
      byte[] r;
      byte[] s;
      DataWord out = null;
      if (sign.length < 65 || Arrays.equals(ZEROADDR,address)
          || Arrays.equals(EMPTYADDR,address)) {
        return false;
      }
      try {
        r = Arrays.copyOfRange(sign, 0, 32);
        s = Arrays.copyOfRange(sign, 32, 64);
        v = sign[64];
        if (v < 27) {
          v += 27;
        }
        ECKey.ECDSASignature signature = ECKey.ECDSASignature.fromComponents(r, s, v);
        if (signature.validateComponents()) {
          out = new DataWord(ECKey.signatureToAddress(hash, signature));
        }
      } catch (Throwable any) {
        logger.info("ECRecover error", any.getMessage());
      }
      return out != null && Arrays.equals(new DataWord(address).getLast20Bytes(),
          out.getLast20Bytes());
    }

    private static byte[][] extractBytes32Array(DataWord[] words, int offset) {
      int len = words[offset].intValueSafe();
      byte[][] bytes32Array = new byte[len][];
      for (int i = 0; i < len; i++) {
        bytes32Array[i] = words[offset + i + 1].getData();
      }
      return bytes32Array;
    }

    private static byte[][] extractBytesArray(DataWord[] words, int offset, byte[] data) {
      int len = words[offset].intValueSafe();
      byte[][] bytesArray = new byte[len][];
      for (int i = 0; i < len; i++) {
        int bytesOffset = words[offset + i + 1].intValueSafe() / DataWord.WORD_SIZE;
        int bytesLen = words[offset + bytesOffset + 1].intValueSafe();
        bytesArray[i] = extractBytes(data,  (bytesOffset + offset + 2) * DataWord.WORD_SIZE,
            bytesLen);
      }
      return bytesArray;
    }

    private static byte[] extractBytes(byte[] data, int offset, int len) {
      return Arrays.copyOfRange(data, offset, offset + len);
    }
  }
}
