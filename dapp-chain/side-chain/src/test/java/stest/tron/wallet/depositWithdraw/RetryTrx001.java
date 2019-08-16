package stest.tron.wallet.depositWithdraw;

import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class RetryTrx001 {


  private final String testDepositTrx = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testDepositAddress = PublicMethed.getFinalAddress(testDepositTrx);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingSideStubFull = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("mainfullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  final String mainGateWayAddress = Configuration.getByPath("testng.conf")
      .getString("gateway_address.key1");
  final String sideGatewayAddress = Configuration.getByPath("testng.conf")
      .getString("gateway_address.key2");

  final String chainIdAddress = Configuration.getByPath("testng.conf")
      .getString("gateway_address.chainIdAddress");
  final byte[] chainIdAddressKey = WalletClient.decodeFromBase58Check(chainIdAddress);

  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] depositAddress = ecKey.getAddress();
  String testKeyFordeposit = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] depositAddress1 = ecKey1.getAddress();
  String testKeyFordeposit1 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] depositAddress2 = ecKey2.getAddress();
  String testKeyFordeposit2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String nonce = null;
  String nonceWithdraw = null;


  private final String testOracle = Configuration.getByPath("testng.conf")
      .getString("oralceAccountKey.key1");
  private final byte[] testOracleAddress = PublicMethed.getFinalAddress(testOracle);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyFordeposit);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingSideStubFull = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Retry Deposit and Withdraw Trx")
  public void test1RetryTrx001() {

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress, 2000000000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainBeforeBalance = accountMainBefore.getBalance();
    Assert.assertTrue(accountMainBeforeBalance == 2000000000L);
    Account accountSideBefore = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();
    ByteString address = accountSideBefore.getAddress();
    String accountSideBeforeAddress = Base58.encode58Check(address.toByteArray());
    logger.info("accountSideBeforeAddress:" + accountSideBeforeAddress);
    Assert.assertEquals("3QJmnh", accountSideBeforeAddress);

    logger.info("accountBeforeBalance:" + accountMainBeforeBalance);
    logger.info("accountSideBeforeBalance:" + accountSideBeforeBalance);

    logger.info("transferTokenContractAddress:" + mainGateWayAddress);
    String methodStr = "depositTRX()";
    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, "", false));

    long callValue = 1500000000;
    String txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(mainGateWayAddress),
            callValue,
            input,
            maxFeeLimit, 0, "", depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("txid:" + txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    long fee = infoById.get().getFee();
    logger.info("fee:" + fee);

    Long nonceLong = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    logger.info("nonce:" + nonceLong);
    nonce = Long.toString(nonceLong);
    Account accountMainAfter = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainAfterBalance = accountMainAfter.getBalance();
    logger.info("accountAfterBalance:" + accountMainAfterBalance);
    Assert.assertEquals(accountMainAfterBalance, accountMainBeforeBalance - fee - 1500000000);
    Account accountSideAfter = PublicMethed.queryAccount(depositAddress, blockingSideStubFull);
    long accountSideAfterBalance = accountSideAfter.getBalance();
    ByteString addressSideAfter = accountSideAfter.getAddress();
    String accountSideAfterAddress = Base58.encode58Check(addressSideAfter.toByteArray());
    logger.info("accountSideAfterAddress:" + accountSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), accountSideAfterAddress);
    Assert.assertEquals(1500000000, accountSideAfterBalance);

    logger.info("sideGatewayAddress:" + sideGatewayAddress);
    long withdrawValue = 1;
    String txid1 = PublicMethed
        .withdrawTrx(chainIdAddress,
            sideGatewayAddress,
            withdrawValue,
            maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("txid1:" + txid1);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingSideStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    long fee1 = infoById1.get().getFee();
    logger.info("fee1:" + fee1);
    Long nonceWithdrawLong = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    logger.info("nonceWithdrawLong:" + nonceWithdrawLong);
    nonceWithdraw = Long.toString(nonceWithdrawLong);

    Account accountSideAfterWithdraw = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountSideAfterWithdrawBalance = accountSideAfterWithdraw.getBalance();
    ByteString addressAfterWithdraw = accountSideAfterWithdraw.getAddress();
    String addressAfterWithdrawAddress = Base58
        .encode58Check(addressAfterWithdraw.toByteArray());
    logger.info("addressAfterWithdrawAddress:" + addressAfterWithdrawAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress), addressAfterWithdrawAddress);
    Assert.assertEquals(accountSideAfterBalance - fee1 - withdrawValue,
        accountSideAfterWithdrawBalance);
    Account accountMainAfterWithdraw = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainAfterWithdrawBalance = accountMainAfterWithdraw.getBalance();
    logger.info("accountAfterWithdrawBalance:" + accountMainAfterWithdrawBalance);
    Assert.assertEquals(accountMainAfterWithdrawBalance,
        accountMainAfterBalance + withdrawValue);

    //retry deposit trx
    String retryDepositTxid = PublicMethed.retryDeposit(mainGateWayAddress,
        nonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("retryDepositTxid:" + retryDepositTxid);
    Optional<TransactionInfo> infoByIdretryDeposit = PublicMethed
        .getTransactionInfoById(retryDepositTxid, blockingStubFull);
    Assert.assertTrue(infoByIdretryDeposit.get().getResultValue() == 0);
    long infoByIdretryDepositFee = infoByIdretryDeposit.get().getFee();
    logger.info("infoByIdretryDepositFee:" + infoByIdretryDepositFee);
    Account accountMainAfterRetry = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainAfterRetryBalance = accountMainAfterRetry.getBalance();
    logger.info("accountMainAfterRetryBalance:" + accountMainAfterRetryBalance);

    Assert.assertEquals(accountMainAfterWithdrawBalance - infoByIdretryDepositFee,
        accountMainAfterRetryBalance);

    Account accountSideAfteRetry = PublicMethed
        .queryAccount(depositAddress, blockingSideStubFull);
    long accountSideAfterRetryBalance = accountSideAfteRetry.getBalance();
    logger.info("accountSideAfterRetryBalance:" + accountSideAfterRetryBalance);

    Assert.assertEquals(accountSideAfterWithdrawBalance,
        accountSideAfterRetryBalance);
    //retry  Withdraw trx

    String retryWithdrawTxid = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        nonceWithdraw,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingSideStubFull);

    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("retryWithdrawTxid:" + retryWithdrawTxid);
    Optional<TransactionInfo> infoByIdretryWithdraw = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid, blockingSideStubFull);
    Assert.assertTrue(infoByIdretryWithdraw.get().getResultValue() == 0);
    long infoByIdretryWithdrawFee = infoByIdretryWithdraw.get().getFee();
    logger.info("infoByIdretryWithdrawFee:" + infoByIdretryWithdrawFee);
  }

  @Test(enabled = true, description = "Retry Deposit and Withdraw Trx with nonce exception ")
  public void test2RetryTrx002() {
    //other account Retry Deposit
    Account accountMainBeforeRetry = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainBeforeRetryBalance = accountMainBeforeRetry.getBalance();
    logger.info("accountMainBeforeRetryBalance:" + accountMainBeforeRetryBalance);
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] depositAddress2 = ecKey2.getAddress();
    String testKeyFordeposit2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress2, 2000000000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String retryDepositTxid1 = PublicMethed.retryDeposit(mainGateWayAddress,
        nonce,
        maxFeeLimit, depositAddress2, testKeyFordeposit2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("retryDepositTxid:" + retryDepositTxid1);
    Optional<TransactionInfo> infoByIdretryDeposit = PublicMethed
        .getTransactionInfoById(retryDepositTxid1, blockingStubFull);
    Assert.assertTrue(infoByIdretryDeposit.get().getResultValue() == 0);
    long infoByIdretryDepositFee = infoByIdretryDeposit.get().getFee();
    logger.info("infoByIdretryDepositFee:" + infoByIdretryDepositFee);
    Account accountMainAfterRetry = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainAfterRetryBalance = accountMainAfterRetry.getBalance();
    logger.info("accountMainAfterRetryBalance:" + accountMainAfterRetryBalance);
    Assert.assertEquals(accountMainBeforeRetryBalance,
        accountMainAfterRetryBalance);

    //other account Retry Withdraw
    String methodStr = "depositTRX()";
    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, "", false));

    long callValue = 1300000000;
    String txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(mainGateWayAddress),
            callValue,
            input,
            maxFeeLimit, 0, "", depositAddress2, testKeyFordeposit2, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    String retryWithdrawTxid = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        nonceWithdraw,
        maxFeeLimit, depositAddress2, testKeyFordeposit2, blockingSideStubFull);
    logger.info("retryWithdrawTxid:" + retryWithdrawTxid);
    Optional<TransactionInfo> infoByIdWithdrawDeposit = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid, blockingStubFull);
    Assert.assertTrue(infoByIdWithdrawDeposit.get().getResultValue() == 0);

    Account accountMainAfterWithdraw = PublicMethed.queryAccount(depositAddress, blockingStubFull);
    long accountMainAfterWithdrawBalance = accountMainAfterWithdraw.getBalance();
    logger.info("accountMainAfterWithdrawBalance:" + accountMainAfterWithdrawBalance);
    Assert.assertEquals(accountMainAfterWithdrawBalance,
        accountMainAfterRetryBalance);
    //Deposit noce value
    String bigNonce = "100000";
    String retryDepositTxid2 = PublicMethed.retryDeposit(mainGateWayAddress,
        bigNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("retryDepositTxid2:" + retryDepositTxid2);
    Optional<TransactionInfo> infoByIdretryDepositTxid2 = PublicMethed
        .getTransactionInfoById(retryDepositTxid2, blockingStubFull);
    Assert.assertTrue(infoByIdretryDepositTxid2.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoByIdretryDepositTxid2.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdretryDepositTxid2.get().getResMessage().toStringUtf8());

    //Withdraw noce value
    String retryWithdrawTxid2 = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        bigNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    logger.info("retryDepositTxid2:" + retryWithdrawTxid2);
    Optional<TransactionInfo> infoByIdretryWithdrawTxid2 = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid2, blockingSideStubFull);
    Assert.assertTrue(infoByIdretryWithdrawTxid2.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoByIdretryWithdrawTxid2.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdretryWithdrawTxid2.get().getResMessage().toStringUtf8());

    //Deposit noce value is 0
    String smallNonce = Long.toString(0);
    logger.info("smallNonce:" + smallNonce);
    String retryDepositTxid3 = PublicMethed.retryDeposit(mainGateWayAddress,
        smallNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("retryDepositTxid3:" + retryDepositTxid3);
    Optional<TransactionInfo> infoByIdretryDepositTxid3 = PublicMethed
        .getTransactionInfoById(retryDepositTxid3, blockingStubFull);
    Assert.assertTrue(infoByIdretryDepositTxid3.get().getResultValue() == 0);

    //Withdraw noce value is 0
    String retryWithdrawTxid3 = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        smallNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    logger.info("retryDepositTxid2:" + retryWithdrawTxid3);
    Optional<TransactionInfo> infoByIdrretryWithdrawTxid3 = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid3, blockingStubFull);
    Assert.assertTrue(infoByIdrretryWithdrawTxid3.get().getResultValue() == 0);

    //Deposit noce value is Long.max_value+1
    String maxNonce = Long.toString(Long.MAX_VALUE + 1);
    logger.info("maxNonce:" + maxNonce);
    String retryDepositTxid4 = PublicMethed.retryDeposit(mainGateWayAddress,
        maxNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("retryDepositTxid4:" + retryDepositTxid4);
    Optional<TransactionInfo> infoByIdretryDepositTxid4 = PublicMethed
        .getTransactionInfoById(retryDepositTxid4, blockingStubFull);
    Assert.assertTrue(infoByIdretryDepositTxid4.get().getResultValue() == 1);
    Assert.assertEquals(FAILED, infoByIdretryDepositTxid4.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdretryDepositTxid4.get().getResMessage().toStringUtf8());

    //Withdraw noce value is Long.max_value+1
    String retryWithdrawTxid4 = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        maxNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    logger.info("retryDepositTxid2:" + retryWithdrawTxid3);
    Optional<TransactionInfo> infoByIdrretryWithdrawTxid4 = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid4, blockingSideStubFull);
    Assert.assertTrue(infoByIdrretryWithdrawTxid4.get().getResultValue() == 1);
    Assert.assertEquals(FAILED, infoByIdrretryWithdrawTxid4.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdrretryWithdrawTxid4.get().getResMessage().toStringUtf8());

    //Deposit noce value is Long.min_value-1
    String minNonce = Long.toString(Long.MIN_VALUE - 1);
    logger.info("maxNonce:" + maxNonce);
    String retryDepositTxid5 = PublicMethed.retryDeposit(mainGateWayAddress,
        minNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("retryDepositTxid4:" + retryDepositTxid5);
    Optional<TransactionInfo> infoByIdretryDepositTxid5 = PublicMethed
        .getTransactionInfoById(retryDepositTxid5, blockingStubFull);
    Assert.assertTrue(infoByIdretryDepositTxid5.get().getResultValue() == 1);
    Assert.assertEquals(FAILED, infoByIdretryDepositTxid5.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdretryDepositTxid5.get().getResMessage().toStringUtf8());

    //Withdraw noce value is Long.min_value-1
    String retryWithdrawTxid5 = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        minNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    logger.info("retryDepositTxid2:" + retryWithdrawTxid5);
    Optional<TransactionInfo> infoByIdrretryWithdrawTxid5 = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid5, blockingSideStubFull);
    Assert.assertTrue(infoByIdrretryWithdrawTxid5.get().getResultValue() == 1);
    Assert.assertEquals(FAILED, infoByIdrretryWithdrawTxid5.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdrretryWithdrawTxid5.get().getResMessage().toStringUtf8());

    //Deposit noce value is Long.min_value-1
    String minusNonce = Long.toString(-1);
    logger.info("minusNonce:" + minusNonce);
    String retryDepositTxid6 = PublicMethed.retryDeposit(mainGateWayAddress,
        minusNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    logger.info("retryDepositTxid4:" + retryDepositTxid6);
    Optional<TransactionInfo> infoByIdretryDepositTxid6 = PublicMethed
        .getTransactionInfoById(retryDepositTxid6, blockingStubFull);
    Assert.assertTrue(infoByIdretryDepositTxid6.get().getResultValue() == 1);
    Assert.assertEquals(FAILED, infoByIdretryDepositTxid6.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdretryDepositTxid6.get().getResMessage().toStringUtf8());

    //Withdraw noce value is -1
    String retryWithdrawTxid6 = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        minusNonce,
        maxFeeLimit, depositAddress, testKeyFordeposit, blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    logger.info("retryWithdrawTxid6:" + retryWithdrawTxid6);
    Optional<TransactionInfo> infoByIdrretryWithdrawTxid6 = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid6, blockingSideStubFull);
    Assert.assertTrue(infoByIdrretryWithdrawTxid6.get().getResultValue() == 1);
    Assert.assertEquals(FAILED, infoByIdrretryWithdrawTxid6.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoByIdrretryWithdrawTxid6.get().getResMessage().toStringUtf8());
  }


  @Test(enabled = true, description = "Retry Deposit and Withdraw Trx with mainOralce value is 0")
  public void test3RetryTrx003() {

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress1, 2000000000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainBeforeBalance = accountMainBefore.getBalance();
    Assert.assertTrue(accountMainBeforeBalance == 2000000000L);
    Account accountSideBefore = PublicMethed.queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();
    ByteString address = accountSideBefore.getAddress();
    String accountSideBeforeAddress = Base58.encode58Check(address.toByteArray());
    logger.info("accountSideBeforeAddress:" + accountSideBeforeAddress);
    Assert.assertEquals("3QJmnh", accountSideBeforeAddress);

    logger.info("accountBeforeBalance:" + accountMainBeforeBalance);
    logger.info("accountSideBeforeBalance:" + accountSideBeforeBalance);

    logger.info("transferTokenContractAddress:" + mainGateWayAddress);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(testOracleAddress, 10000000,
        0, 0, testOracle, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account oracleMainBeforeSend = PublicMethed.queryAccount(testOracleAddress, blockingStubFull);
    long oracleMainBeforeSendBalance = oracleMainBeforeSend.getBalance();

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress2, oracleMainBeforeSendBalance, testOracleAddress, testOracle,
            blockingStubFull));
    AccountResourceMessage oracleMainBeforeWithdraw = PublicMethed
        .getAccountResource(testOracleAddress,
            blockingStubFull);
    long oracleMainBeforeWithdrawnEnergyLimit = oracleMainBeforeWithdraw.getEnergyLimit();
    long oracleMainBeforeWithdrawEnergyUsage = oracleMainBeforeWithdraw.getEnergyUsed();
    long oracleMainBeforeWithdrawNetUsed = oracleMainBeforeWithdraw.getNetUsed();
    long oracleMainBeforeWithdrawNetLimit = oracleMainBeforeWithdraw.getNetLimit();
    Assert.assertEquals(oracleMainBeforeWithdrawnEnergyLimit, 0);
    Assert.assertEquals(oracleMainBeforeWithdrawEnergyUsage, 0);
    Assert.assertTrue(oracleMainBeforeWithdrawNetUsed < oracleMainBeforeWithdrawNetLimit);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String methodStr = "depositTRX()";
    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, "", false));

    long callValue = 1500000000;
    String txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(mainGateWayAddress),
            callValue,
            input,
            maxFeeLimit, 0, "", depositAddress1, testKeyFordeposit1, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("txid:" + txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    long fee = infoById.get().getFee();
    logger.info("fee:" + fee);

    Long nonceLong = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    logger.info("nonce:" + nonceLong);
    nonce = Long.toString(nonceLong);
    Account accountMainAfter = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterBalance = accountMainAfter.getBalance();
    logger.info("accountAfterBalance:" + accountMainAfterBalance);
    Assert.assertEquals(accountMainAfterBalance, accountMainBeforeBalance - fee - 1500000000);
    Account accountSideAfter = PublicMethed.queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterBalance = accountSideAfter.getBalance();
    ByteString addressSideAfter = accountSideAfter.getAddress();
    String accountSideAfterAddress = Base58.encode58Check(addressSideAfter.toByteArray());
    logger.info("accountSideAfterAddress:" + accountSideAfterAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress1), accountSideAfterAddress);
    Assert.assertEquals(1500000000, accountSideAfterBalance);

    logger.info("sideGatewayAddress:" + sideGatewayAddress);

    long withdrawValue = 1;
    String txid1 = PublicMethed
        .withdrawTrx(chainIdAddress,
            sideGatewayAddress,
            withdrawValue,
            maxFeeLimit, depositAddress1, testKeyFordeposit1, blockingStubFull,
            blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("txid1:" + txid1);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingSideStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    long fee1 = infoById1.get().getFee();
    logger.info("fee1:" + fee1);
    Long nonceWithdrawLong = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    logger.info("nonceWithdrawLong:" + nonceWithdrawLong);
    nonceWithdraw = Long.toString(nonceWithdrawLong);
    logger.info("nonceWithdraw:" + nonceWithdraw);

    Account accountSideAfterWithdraw = PublicMethed
        .queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterWithdrawBalance = accountSideAfterWithdraw.getBalance();
    ByteString addressAfterWithdraw = accountSideAfterWithdraw.getAddress();
    String addressAfterWithdrawAddress = Base58
        .encode58Check(addressAfterWithdraw.toByteArray());
    logger.info("addressAfterWithdrawAddress:" + addressAfterWithdrawAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress1), addressAfterWithdrawAddress);
    Assert.assertEquals(accountSideAfterBalance - fee1 - 1,
        accountSideAfterWithdrawBalance);
    Account accountMainAfterWithdraw = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterWithdrawBalance = accountMainAfterWithdraw.getBalance();
    logger.info("accountAfterWithdrawBalance:" + accountMainAfterWithdrawBalance);
    Assert.assertEquals(accountMainAfterWithdrawBalance,
        accountMainAfterBalance);

    //retry  Withdraw trx

    Assert.assertTrue(PublicMethed
        .sendcoin(testOracleAddress, oracleMainBeforeSendBalance - 200000, depositAddress2,
            testKeyFordeposit2,
            blockingStubFull));
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Account oracleMainAfterSend = PublicMethed.queryAccount(testOracleAddress, blockingStubFull);
    long oracleMainAfterSendBalance = oracleMainAfterSend.getBalance();
    logger.info("oracleMainAfterSendBalance:" + oracleMainAfterSendBalance);

    String retryWithdrawTxid = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        nonceWithdraw,
        maxFeeLimit, depositAddress1, testKeyFordeposit1, blockingSideStubFull);
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    logger.info("retryWithdrawTxid:" + retryWithdrawTxid);
    Optional<TransactionInfo> infoByIdretryWithdraw = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid, blockingSideStubFull);
    Assert.assertTrue(infoByIdretryWithdraw.get().getResultValue() == 0);
    long infoByIdretryWithdrawFee = infoByIdretryWithdraw.get().getFee();
    logger.info("infoByIdretryWithdrawFee:" + infoByIdretryWithdrawFee);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountMainAfterRetryWithdraw = PublicMethed
        .queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterRetryWithdrawBalance = accountMainAfterRetryWithdraw.getBalance();
    Account accountSideAfterRetryWithdraw = PublicMethed
        .queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterRetryWithdrawBalance = accountSideAfterRetryWithdraw.getBalance();
    Assert.assertEquals(accountSideAfterWithdrawBalance - infoByIdretryWithdrawFee,
        accountSideAfterRetryWithdrawBalance);
    Assert.assertEquals(accountMainAfterWithdrawBalance + 1,
        accountMainAfterRetryWithdrawBalance);
  }


  @Test(enabled = true, description = "Retry Deposit and Withdraw Trx with sideOralce value is 0")
  public void test4RetryTrx004() {
    PublicMethed.printAddress(testKeyFordeposit);
    PublicMethed.printAddress(testKeyFordeposit1);
    PublicMethed.printAddress(testKeyFordeposit2);

    Assert.assertTrue(PublicMethed
        .sendcoin(depositAddress1, 2000000000L, testDepositAddress, testDepositTrx,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountMainBefore = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainBeforeBalance = accountMainBefore.getBalance();
    Account accountSideBefore = PublicMethed.queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideBeforeBalance = accountSideBefore.getBalance();
    ByteString address = accountSideBefore.getAddress();
    String accountSideBeforeAddress = Base58.encode58Check(address.toByteArray());
    logger.info("accountSideBeforeAddress:" + accountSideBeforeAddress);

    logger.info("accountBeforeBalance:" + accountMainBeforeBalance);
    logger.info("accountSideBeforeBalance:" + accountSideBeforeBalance);

    logger.info("mainGateWayAddress:" + mainGateWayAddress);
    Assert.assertTrue(PublicMethed.freezeBalanceSideChainGetEnergy(testOracleAddress, 100000000,
        3, 0, testOracle, chainIdAddressKey, blockingSideStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account oracleMainBeforeSend = PublicMethed
        .queryAccount(testOracleAddress, blockingSideStubFull);
    long oracleMainBeforeSendBalance = oracleMainBeforeSend.getBalance();

    Assert.assertTrue(PublicMethed
        .sendcoinForSidechain(depositAddress2, oracleMainBeforeSendBalance, testOracleAddress,
            testOracle, chainIdAddressKey,
            blockingSideStubFull));
    AccountResourceMessage oracleSideBeforeWithdraw = PublicMethed
        .getAccountResource(testOracleAddress,
            blockingSideStubFull);
    long oracleSideBeforeWithdrawnEnergyLimit = oracleSideBeforeWithdraw.getEnergyLimit();
    long oracleSideBeforeWithdrawEnergyUsage = oracleSideBeforeWithdraw.getEnergyUsed();
    long oracleSideBeforeWithdrawNetUsed = oracleSideBeforeWithdraw.getNetUsed();
    long oracleSideBeforeWithdrawNetLimit = oracleSideBeforeWithdraw.getNetLimit();
    Assert.assertEquals(oracleSideBeforeWithdrawnEnergyLimit, 0);
    Assert.assertEquals(oracleSideBeforeWithdrawEnergyUsage, 0);
    Assert.assertTrue(oracleSideBeforeWithdrawNetUsed < oracleSideBeforeWithdrawNetLimit);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);

    String methodStr = "depositTRX()";
    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, "", false));

    long callValue = 1500000000;
    String txid = PublicMethed
        .triggerContract(WalletClient.decodeFromBase58Check(mainGateWayAddress),
            callValue,
            input,
            maxFeeLimit, 0, "", depositAddress1, testKeyFordeposit1, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("txid:" + txid);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    long fee = infoById.get().getFee();
    logger.info("fee:" + fee);

    Long nonceLong = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    logger.info("nonce:" + nonceLong);
    nonce = Long.toString(nonceLong);
    Account accountMainAfter = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterBalance = accountMainAfter.getBalance();
    logger.info("accountAfterBalance:" + accountMainAfterBalance);
    ByteString addressMainAfter = accountMainAfter.getAddress();
    String accountMainAfterAddress = Base58.encode58Check(addressMainAfter.toByteArray());
    logger.info("accountMainAfterAddress:" + accountMainAfterAddress);
    Assert.assertEquals(accountMainAfterBalance, accountMainBeforeBalance - fee - 1500000000);
    Account accountSideAfter = PublicMethed.queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterBalance = accountSideAfter.getBalance();
    ByteString addressSideAfter = accountSideAfter.getAddress();
    String accountSideAfterAddress = Base58.encode58Check(addressSideAfter.toByteArray());
    logger.info("accountSideAfterAddress:" + accountSideAfterAddress);
//    Assert.assertEquals(Base58.encode58Check(depositAddress1), accountSideAfterAddress);
    Assert.assertEquals(accountSideBeforeBalance, accountSideAfterBalance);

    Assert.assertTrue(PublicMethed
        .sendcoinForSidechain(testOracleAddress, oracleMainBeforeSendBalance - 200000,
            depositAddress2,
            testKeyFordeposit2, chainIdAddressKey,
            blockingSideStubFull));
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //retry deposit trx
    String retryDepositTxid = PublicMethed.retryDeposit(mainGateWayAddress,
        nonce,
        maxFeeLimit, depositAddress1, testKeyFordeposit1, blockingStubFull);
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    logger.info("retryDepositTxid:" + retryDepositTxid);
    Optional<TransactionInfo> infoByIdretryDeposit = PublicMethed
        .getTransactionInfoById(retryDepositTxid, blockingStubFull);
    Assert.assertTrue(infoByIdretryDeposit.get().getResultValue() == 0);
    long infoByIdretryDepositFee = infoByIdretryDeposit.get().getFee();
    logger.info("infoByIdretryDepositFee:" + infoByIdretryDepositFee);
    Account accountMainAfterRetry = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterRetryBalance = accountMainAfterRetry.getBalance();
    logger.info("accountMainAfterRetryBalance:" + accountMainAfterRetryBalance);

    Assert.assertEquals(accountMainAfterBalance - infoByIdretryDepositFee,
        accountMainAfterRetryBalance);

    Account accountSideAfteRetry = PublicMethed
        .queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterRetryBalance = accountSideAfteRetry.getBalance();
    logger.info("accountSideAfterRetryBalance:" + accountSideAfterRetryBalance);

    Assert.assertEquals(accountSideAfterBalance + callValue,
        accountSideAfterRetryBalance);

    Account oracleSideBeforeSend = PublicMethed
        .queryAccount(testOracleAddress, blockingSideStubFull);
    long oracleSideBeforeSendBalance = oracleSideBeforeSend.getBalance();
    Assert.assertTrue(PublicMethed
        .sendcoinForSidechain(depositAddress2, oracleSideBeforeSendBalance, testOracleAddress,
            testOracle, chainIdAddressKey,
            blockingSideStubFull));

//withdrawTrx
    logger.info("sideGatewayAddress:" + sideGatewayAddress);

    long withdrawValue = 1;
    String txid1 = PublicMethed
        .withdrawTrx(chainIdAddress,
            sideGatewayAddress,
            withdrawValue,
            maxFeeLimit, depositAddress1, testKeyFordeposit1, blockingStubFull,
            blockingSideStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingSideStubFull);
    logger.info("txid1:" + txid1);
    Optional<TransactionInfo> infoById1 = PublicMethed
        .getTransactionInfoById(txid1, blockingSideStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    long fee1 = infoById1.get().getFee();
    logger.info("fee1:" + fee1);
    Long nonceWithdrawLong = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    logger.info("nonceWithdrawLong:" + nonceWithdrawLong);
    nonceWithdraw = Long.toString(nonceWithdrawLong);
    logger.info("nonceWithdraw:" + nonceWithdraw);

    Account accountSideAfterWithdraw = PublicMethed
        .queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterWithdrawBalance = accountSideAfterWithdraw.getBalance();
    ByteString addressAfterWithdraw = accountSideAfterWithdraw.getAddress();
    String addressAfterWithdrawAddress = Base58
        .encode58Check(addressAfterWithdraw.toByteArray());
    logger.info("addressAfterWithdrawAddress:" + addressAfterWithdrawAddress);
    Assert.assertEquals(Base58.encode58Check(depositAddress1), addressAfterWithdrawAddress);
    Assert.assertEquals(accountSideAfterRetryBalance - fee1 - withdrawValue,
        accountSideAfterWithdrawBalance);
    Account accountMainAfterWithdraw = PublicMethed.queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterWithdrawBalance = accountMainAfterWithdraw.getBalance();
    logger.info("accountAfterWithdrawBalance:" + accountMainAfterWithdrawBalance);
    Assert.assertEquals(accountMainAfterWithdrawBalance,
        accountMainAfterRetryBalance);

    Assert.assertTrue(PublicMethed
        .sendcoinForSidechain(testOracleAddress, oracleSideBeforeSendBalance - 100000,
            depositAddress2,
            testKeyFordeposit2, chainIdAddressKey,
            blockingSideStubFull));
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    //retry  Withdraw trx

    Account oracleMainAfterSend = PublicMethed
        .queryAccount(testOracleAddress, blockingSideStubFull);
    long oracleMainAfterSendBalance = oracleMainAfterSend.getBalance();
    logger.info("oracleMainAfterSendBalance:" + oracleMainAfterSendBalance);

    String retryWithdrawTxid = PublicMethed.retryWithdraw(chainIdAddress, sideGatewayAddress,
        nonceWithdraw,
        maxFeeLimit, depositAddress1, testKeyFordeposit1, blockingSideStubFull);
    try {
      Thread.sleep(60000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
//
    logger.info("retryWithdrawTxid:" + retryWithdrawTxid);
    Optional<TransactionInfo> infoByIdretryWithdraw = PublicMethed
        .getTransactionInfoById(retryWithdrawTxid, blockingSideStubFull);
    Assert.assertTrue(infoByIdretryWithdraw.get().getResultValue() == 0);
    long infoByIdretryWithdrawFee = infoByIdretryWithdraw.get().getFee();
    logger.info("infoByIdretryWithdrawFee:" + infoByIdretryWithdrawFee);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account accountMainAfterRetryWithdraw = PublicMethed
        .queryAccount(depositAddress1, blockingStubFull);
    long accountMainAfterRetryWithdrawBalance = accountMainAfterRetryWithdraw.getBalance();
    Account accountSideAfterRetryWithdraw = PublicMethed
        .queryAccount(depositAddress1, blockingSideStubFull);
    long accountSideAfterRetryWithdrawBalance = accountSideAfterRetryWithdraw.getBalance();
    Assert.assertEquals(accountSideAfterWithdrawBalance - infoByIdretryWithdrawFee,
        accountSideAfterRetryWithdrawBalance);
    Assert.assertEquals(accountMainAfterWithdrawBalance + withdrawValue,
        accountMainAfterRetryWithdrawBalance);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
