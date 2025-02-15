package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j
public class ContractTrcToken036 {
  private final String tokenOwnerKey = Configuration.getByPath("testng.conf")
      .getString("tokenFoundationAccount.slideTokenOwnerKey");
  private final byte[] tokenOnwerAddress = PublicMethedForDailybuild.getFinalAddress(tokenOwnerKey);
  private final String tokenId = Configuration.getByPath("testng.conf")
      .getString("tokenFoundationAccount.slideTokenId");



  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private static ByteString assetAccountId = null;
  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  byte[] transferTokenContractAddress;
  private static final long TotalSupply = 10000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  int originEnergyLimit = 50000;
  byte[] transferTokenWithPureTestAddress;

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

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    assetAccountId = ByteString.copyFromUtf8(tokenId);
    Assert.assertTrue(PublicMethedForDailybuild.transferAsset(dev001Address, assetAccountId.toByteArray(),
        10000000L, tokenOnwerAddress, tokenOwnerKey, blockingStubFull));

  }


  @Test(enabled = false, description = "Deploy contract")
  public void deploy01TransferTokenContract() {

    Assert
        .assertTrue(PublicMethedForDailybuild.sendcoin(dev001Address, 9999000000L, fromAddress,
            testKey002, blockingStubFull));
    logger.info(
        "dev001Address:" + Base58.encode58Check(dev001Address));
    Assert
        .assertTrue(PublicMethedForDailybuild.sendcoin(user001Address, 4048000000L, fromAddress,
            testKey002, blockingStubFull));
    logger.info(
        "user001Address:" + Base58.encode58Check(user001Address));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // freeze balance
    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceGetEnergy(dev001Address, 204800000,
        0, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethedForDailybuild.freezeBalanceGetEnergy(user001Address, 2048000000,
        0, 1, user001Key, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);


    // deploy transferTokenContract
    //    String filePath = "src/test/resources/soliditycode/contractTrcToken036.sol";
    //    String contractName = "IllegalDecorate";
    //    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    //    String code = retMap.get("byteCode").toString();
    //    String abi = retMap.get("abI").toString();
    //    transferTokenContractAddress = PublicMethedForDailybuild
    //        .deployContract(contractName, abi, code, "", maxFeeLimit,
    //            0L, 0, originEnergyLimit, "0",
    //            0, null, dev001Key, dev001Address,
    //            blockingStubFull);
    //    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    //    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    //
    //    // devAddress transfer token to userAddress
    //    PublicMethedForDailybuild
    //        .transferAsset(transferTokenContractAddress, assetAccountId.toByteArray(), 100,
    //            dev001Address,
    //            dev001Key,
    //            blockingStubFull);
    //    Assert
    //        .assertTrue(PublicMethedForDailybuild.sendcoin(transferTokenContractAddress, 100, fromAddress,
    //            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithPure contract")
  public void deploy02TransferTokenContract() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    info = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance = info.getBalance();
    Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
    Long beforeNetUsed = resourceInfo.getNetUsed();
    Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    Long beforeAssetIssueDevAddress = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long beforeAssetIssueUserAddress = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);

    Long beforeAssetIssueContractAddress = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
    Long user001AddressAddressBalance = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance);

    // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    final String triggerTxid = PublicMethedForDailybuild.triggerContract(transferTokenContractAddress,
        "transferTokenWithPure(address,uint256)",
        param, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account infoafter = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance = infoafter.getBalance();
    Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    Long afterAssetIssueDevAddress = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long afterNetUsed = resourceInfoafter.getNetUsed();
    Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    Long afterAssetIssueContractAddress = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
    Long afterAssetIssueUserAddress = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
    Long afteruser001AddressAddressBalance = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterContractAddressBalance:" + afteruser001AddressAddressBalance);

    Optional<TransactionInfo> infoById = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(beforeAssetIssueDevAddress - 10 == afterAssetIssueDevAddress);
    Assert.assertTrue(beforeAssetIssueUserAddress + 10 == afterAssetIssueUserAddress);
    Assert.assertTrue(user001AddressAddressBalance + 10 == afteruser001AddressAddressBalance);

    String filePath = "src/test/resources/soliditycode/contractTrcToken036.sol";
    String contractName1 = "IllegalDecorate1";
    HashMap retMap1 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    transferTokenWithPureTestAddress = PublicMethedForDailybuild
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethedForDailybuild
        .transferAsset(transferTokenWithPureTestAddress, assetAccountId.toByteArray(), 100,
            dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethedForDailybuild.sendcoin(transferTokenWithPureTestAddress, 100, fromAddress,
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithConstant contract")
  public void deploy03TransferTokenContract() {
    Account info1;
    AccountResourceMessage resourceInfo1 = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    info1 = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance1 = info1.getBalance();
    Long beforeEnergyUsed1 = resourceInfo1.getEnergyUsed();
    Long beforeNetUsed1 = resourceInfo1.getNetUsed();
    Long beforeFreeNetUsed1 = resourceInfo1.getFreeNetUsed();
    Long beforeAssetIssueDevAddress1 = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long beforeAssetIssueUserAddress1 = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);

    Long beforeAssetIssueContractAddress1 = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
    Long user001AddressAddressBalance1 = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance1);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed1);
    logger.info("beforeNetUsed:" + beforeNetUsed1);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed1);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress1);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress1);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress1);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance1);

    // user trigger A to transfer token to B
    String param1 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    final String triggerTxid1 = PublicMethedForDailybuild.triggerContract(transferTokenWithPureTestAddress,
        "transferTokenWithConstant(address,uint256)",
        param1, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account infoafter1 = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance1 = infoafter1.getBalance();
    Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
    Long afterAssetIssueDevAddress1 = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
    Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
    Long afterAssetIssueContractAddress1 = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenContractAddress,
            assetAccountId,
            blockingStubFull);
    Long afterAssetIssueUserAddress1 = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
    Long afteruser001AddressAddressBalance1 = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance1);
    logger.info("afterEnergyUsed:" + afterEnergyUsed1);
    logger.info("afterNetUsed:" + afterNetUsed1);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed1);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress1);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress1);
    logger.info("afterContractAddressBalance:" + afteruser001AddressAddressBalance1);

    Optional<TransactionInfo> infoById1 = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertEquals(beforeBalance1, afterBalance1);
    Assert.assertEquals(beforeAssetIssueDevAddress1, afterAssetIssueDevAddress1);
    Assert.assertEquals(beforeAssetIssueUserAddress1, afterAssetIssueUserAddress1);
    Assert.assertEquals(user001AddressAddressBalance1, afteruser001AddressAddressBalance1);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithView contract")
  public void deploy04TransferTokenContract() {
    String filePath2 = "src/test/resources/soliditycode/contractTrcToken036.sol";
    String contractName2 = "IllegalDecorate2";
    HashMap retMap2 = PublicMethedForDailybuild.getBycodeAbi(filePath2, contractName2);

    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abI").toString();
    byte[] transferTokenWithViewAddress = PublicMethedForDailybuild
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    // devAddress transfer token to userAddress
    PublicMethedForDailybuild
        .transferAsset(transferTokenWithViewAddress, assetAccountId.toByteArray(), 100,
            dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethedForDailybuild.sendcoin(transferTokenWithViewAddress, 100, fromAddress,
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account info2;
    AccountResourceMessage resourceInfo2 = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    info2 = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance2 = info2.getBalance();
    Long beforeEnergyUsed2 = resourceInfo2.getEnergyUsed();
    Long beforeNetUsed2 = resourceInfo2.getNetUsed();
    Long beforeFreeNetUsed2 = resourceInfo2.getFreeNetUsed();
    Long beforeAssetIssueDevAddress2 = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long beforeAssetIssueUserAddress2 = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);

    Long beforeAssetIssueContractAddress2 = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenWithViewAddress,
            assetAccountId,
            blockingStubFull);
    Long user001AddressAddressBalance2 = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeAssetIssueContractAddress2:" + beforeAssetIssueContractAddress2);
    logger.info("beforeAssetIssueDevAddress2:" + beforeAssetIssueDevAddress2);
    logger.info("beforeAssetIssueUserAddress2:" + beforeAssetIssueUserAddress2);
    logger.info("user001AddressAddressBalance2:" + user001AddressAddressBalance2);

    // user trigger A to transfer token to B
    String param2 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    String triggerTxid2 = PublicMethedForDailybuild.triggerContract(transferTokenWithViewAddress,
        "transferTokenWithView(address,uint256)",
        param2, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);

    Account infoafter2 = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter2 = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance2 = infoafter2.getBalance();
    Long afterEnergyUsed2 = resourceInfoafter2.getEnergyUsed();
    Long afterAssetIssueDevAddress2 = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long afterNetUsed2 = resourceInfoafter2.getNetUsed();
    Long afterFreeNetUsed2 = resourceInfoafter2.getFreeNetUsed();
    Long afterAssetIssueContractAddress2 = PublicMethedForDailybuild
        .getAssetIssueValue(transferTokenWithViewAddress,
            assetAccountId,
            blockingStubFull);
    Long afterAssetIssueUserAddress2 = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
    Long afteruser001AddressAddressBalance2 = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("afterAssetIssueDevAddress2:" + afterAssetIssueDevAddress2);
    logger.info("afterAssetIssueContractAddress2:" + afterAssetIssueContractAddress2);
    logger.info("afterAssetIssueUserAddress2:" + afterAssetIssueUserAddress2);
    logger.info("afteruser001AddressAddressBalance2:" + afteruser001AddressAddressBalance2);

    Optional<TransactionInfo> infoById2 = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid2, blockingStubFull);

    Assert.assertEquals(beforeAssetIssueDevAddress2, afterAssetIssueDevAddress2);
    Assert.assertEquals(beforeAssetIssueUserAddress2, afterAssetIssueUserAddress2);
    Assert.assertEquals(user001AddressAddressBalance2, afteruser001AddressAddressBalance2);
  }

  @Test(enabled = false, description = "Trigger transferTokenWithNoPayable contract")
  public void deploy05TransferTokenContract() {
    String filePath = "src/test/resources/soliditycode/contractTrcToken036.sol";
    String contractName3 = "IllegalDecorate3";
    HashMap retMap3 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName3);

    String code3 = retMap3.get("byteCode").toString();
    String abi3 = retMap3.get("abI").toString();
    byte[] transferTokenWithOutPayableTestAddress = PublicMethedForDailybuild
        .deployContract(contractName3, abi3, code3, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    PublicMethedForDailybuild
        .transferAsset(transferTokenWithOutPayableTestAddress, assetAccountId.toByteArray(), 100,
            dev001Address,
            dev001Key,
            blockingStubFull);
    Assert
        .assertTrue(PublicMethedForDailybuild.sendcoin(transferTokenWithOutPayableTestAddress, 100, fromAddress,
            testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Account info3;
    AccountResourceMessage resourceInfo3 = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    info3 = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    Long beforeBalance3 = info3.getBalance();
    Long beforeEnergyUsed3 = resourceInfo3.getEnergyUsed();
    Long beforeNetUsed3 = resourceInfo3.getNetUsed();
    Long beforeFreeNetUsed3 = resourceInfo3.getFreeNetUsed();
    Long beforeAssetIssueDevAddress3 = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long beforeAssetIssueUserAddress3 = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);

    Long beforeAssetIssueContractAddress3 = PublicMethedForDailybuild
        .getAssetIssueValue(
            transferTokenWithOutPayableTestAddress,
            assetAccountId,
            blockingStubFull);
    Long user001AddressAddressBalance3 = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();
    logger.info("beforeBalance:" + beforeBalance3);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed3);
    logger.info("beforeNetUsed:" + beforeNetUsed3);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed3);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress3);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress3);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress3);
    logger.info("user001AddressAddressBalance:" + user001AddressAddressBalance3);

    String param3 =
        "\"" + Base58.encode58Check(user001Address) + "\",\"1\"";

    String triggerTxid3 = PublicMethedForDailybuild.triggerContract(transferTokenWithOutPayableTestAddress,
        "transferTokenWithOutPayable(address,uint256)",
        param3, false, 10, 1000000000L, assetAccountId
            .toStringUtf8(),
        10, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Account infoafter3 = PublicMethedForDailybuild.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter3 = PublicMethedForDailybuild.getAccountResource(dev001Address,
        blockingStubFull);
    Long afterBalance3 = infoafter3.getBalance();
    Long afterEnergyUsed3 = resourceInfoafter3.getEnergyUsed();
    Long afterAssetIssueDevAddress3 = PublicMethedForDailybuild
        .getAssetIssueValue(dev001Address, assetAccountId,
            blockingStubFull);
    Long afterNetUsed3 = resourceInfoafter3.getNetUsed();
    Long afterFreeNetUsed3 = resourceInfoafter3.getFreeNetUsed();
    Long afterAssetIssueContractAddress3 = PublicMethedForDailybuild
        .getAssetIssueValue(
            transferTokenWithOutPayableTestAddress, assetAccountId,
            blockingStubFull);
    Long afterAssetIssueUserAddress3 = PublicMethedForDailybuild
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
    Long afteruser001AddressAddressBalance3 = PublicMethedForDailybuild
        .queryAccount(user001Address, blockingStubFull).getBalance();

    Optional<TransactionInfo> infoById3 = PublicMethedForDailybuild
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 1);

    Assert.assertEquals(beforeAssetIssueDevAddress3, afterAssetIssueDevAddress3);
    Assert.assertEquals(beforeAssetIssueUserAddress3, afterAssetIssueUserAddress3);
    Assert.assertEquals(user001AddressAddressBalance3, afteruser001AddressAddressBalance3);
    PublicMethedForDailybuild.unFreezeBalance(dev001Address, dev001Key, 1,
        null, blockingStubFull);
    PublicMethedForDailybuild.unFreezeBalance(user001Address, user001Key, 1,
        null, blockingStubFull);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}


