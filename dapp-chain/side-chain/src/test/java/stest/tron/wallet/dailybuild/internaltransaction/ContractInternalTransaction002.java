package stest.tron.wallet.dailybuild.internaltransaction;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;

@Slf4j

public class ContractInternalTransaction002 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethedForDailybuild.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForinternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


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
    PublicMethedForDailybuild.printAddress(testKeyForinternalTxsAddress);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    logger.info(Long.toString(PublicMethedForDailybuild.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));
  }


  @Test(enabled = true, description = "Type is create create call call")
  public void test1InternalTransaction007() {
    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction002test1InternalTransaction007.sol";
    String contractName = "A";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethedForDailybuild.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "C";
    HashMap retMap1 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethedForDailybuild
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";

    String txid = "";

    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "test1(address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertNotNull(infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertTrue(infoById.get().getInternalTransactions(i).getRejected());
    }
    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(0 == vaule2);
    Assert.assertTrue(5 == vaule3);
    Assert.assertTrue(0 == vaule4);
    String initParmes1 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid1 = PublicMethedForDailybuild.triggerContract(contractAddress,
        "test2(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethedForDailybuild.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(1, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    String note5 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    Long vaule5 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(1 == vaule5);
    Assert.assertEquals("call", note5);
    Assert.assertTrue(infoById1.get().getInternalTransactions(0).getRejected());


  }

  @Test(enabled = true, description = "Type is call call")
  public void test2InternalTransaction008() {
    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction002test2InternalTransaction008.sol";
    String contractName = "A";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethedForDailybuild.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "B";
    HashMap retMap1 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethedForDailybuild
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    String txid = "";
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testAssert(address,uint256)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    Assert.assertTrue(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertFalse(infoById.get().getInternalTransactions(1).getRejected());

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note);
    Assert.assertEquals("call", note1);
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(1 == vaule2);
    String contractName2 = "C";
    HashMap retMap2 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName2);
    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abI").toString();

    byte[] contractAddress2 = PublicMethedForDailybuild
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String initParmes1 = "\"" + Base58.encode58Check(contractAddress2) + "\",\"1\"";
    String txid1 = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testRequire(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethedForDailybuild.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());
    Assert.assertTrue(infoById1.get().getInternalTransactions(0).getRejected());
    Assert.assertFalse(infoById1.get().getInternalTransactions(1).getRejected());
    String note2 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(0).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById1.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule3 = infoById1.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById1.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);
    Assert.assertTrue(1 == vaule3);
    Assert.assertTrue(1 == vaule4);

    String txid2 = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testAssert1(address,uint256)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethedForDailybuild.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    int transactionsCount2 = infoById2.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount2);
    dupInternalTrsansactionHash(infoById2.get().getInternalTransactionsList());
    Assert.assertFalse(infoById2.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById2.get().getInternalTransactions(1).getRejected());

    String note5 = ByteArray
        .toStr(infoById2.get().getInternalTransactions(0).getNote().toByteArray());
    String note6 = ByteArray
        .toStr(infoById2.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule5 = infoById2.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule6 = infoById2.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note5);
    Assert.assertEquals("call", note6);
    Assert.assertTrue(1 == vaule5);
    Assert.assertTrue(1 == vaule6);

    String txid3 = PublicMethedForDailybuild.triggerContract(contractAddress,
        "testtRequire2(address,uint256)", initParmes1, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethedForDailybuild.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);
    int transactionsCount3 = infoById3.get().getInternalTransactionsCount();
    Assert.assertEquals(2, transactionsCount3);
    dupInternalTrsansactionHash(infoById3.get().getInternalTransactionsList());

    Assert.assertFalse(infoById3.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById3.get().getInternalTransactions(1).getRejected());
    String note7 = ByteArray
        .toStr(infoById3.get().getInternalTransactions(0).getNote().toByteArray());
    String note8 = ByteArray
        .toStr(infoById3.get().getInternalTransactions(1).getNote().toByteArray());
    Long vaule7 = infoById3.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule8 = infoById3.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("call", note7);
    Assert.assertEquals("call", note8);
    Assert.assertTrue(1 == vaule7);
    Assert.assertTrue(1 == vaule8);

  }

  @Test(enabled = true, description = "Test suicide type in internalTransaction after call")
  public void test3InternalTransaction009() {
    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction002test3InternalTransaction009.sol";
    String contractName = "A";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethedForDailybuild.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "B";
    HashMap retMap1 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethedForDailybuild
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "C";
    HashMap retMap2 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName2);
    String code2 = retMap2.get("byteCode").toString();
    String abi2 = retMap2.get("abI").toString();
    byte[] contractAddress2 = PublicMethedForDailybuild
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String contractName3 = "D";
    HashMap retMap3 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName3);
    String code3 = retMap3.get("byteCode").toString();
    String abi3 = retMap3.get("abI").toString();
    byte[] contractAddress3 = PublicMethedForDailybuild
        .deployContract(contractName3, abi3, code3, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String initParmes = "\"" + Base58.encode58Check(contractAddress2)
        + "\",\"" + Base58.encode58Check(contractAddress3) + "\",\"" + Base58
        .encode58Check(contractAddress1) + "\"";
    String txid = "";
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "test1(address,address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(7, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(6).getNote().toByteArray());
    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals("create", note);
    Assert.assertEquals("call", note1);
    Assert.assertEquals("suicide", note2);
    Assert.assertTrue(10 == vaule1);
    Assert.assertTrue(5 == vaule2);

    String txid1 = "";
    txid1 = PublicMethedForDailybuild.triggerContract(contractAddress,
        "test1(address,address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethedForDailybuild.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(6, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }
  }

  @Test(enabled = false, description = "Test maxfeelimit can trigger create type max time")
  public void test4InternalTransaction010() {
    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction002test4InternalTransaction010.sol";
    String contractName = "A";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress = PublicMethedForDailybuild.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String txid = "";
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "transfer()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(76, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());
    }
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String txid1 = PublicMethedForDailybuild.triggerContract(contractAddress,
        "transfer2()", "#", false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethedForDailybuild.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 1);
    int transactionsCount1 = infoById1.get().getInternalTransactionsCount();
    Assert.assertEquals(76, transactionsCount1);
    dupInternalTrsansactionHash(infoById1.get().getInternalTransactionsList());

    for (int i = 0; i < transactionsCount1; i++) {
      Assert.assertTrue(infoById1.get().getInternalTransactions(i).getRejected());
      Assert.assertEquals("create", ByteArray
          .toStr(infoById1.get().getInternalTransactions(i).getNote().toByteArray()));
      Assert.assertEquals(1,
          infoById1.get().getInternalTransactions(i).getCallValueInfo(0).getCallValue());

    }


  }


  @Test(enabled = true, description = "Type is call create->call->call.Three-level nesting")
  public void test5InternalTransaction012() {
    Assert.assertTrue(PublicMethedForDailybuild
        .sendcoin(internalTxsAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/"
        + "contractInternalTransaction002test5InternalTransaction012.sol";
    String contractName = "A";
    HashMap retMap = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    contractAddress = PublicMethedForDailybuild.deployContract(contractName, abi, code, "", maxFeeLimit,
        1000000L, 100, null, testKeyForinternalTxsAddress,
        internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    String contractName1 = "B";
    HashMap retMap1 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    byte[] contractAddress1 = PublicMethedForDailybuild
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String contractName2 = "E";
    HashMap retMap2 = PublicMethedForDailybuild.getBycodeAbi(filePath, contractName2);
    String code2 = retMap1.get("byteCode").toString();
    String abi2 = retMap1.get("abI").toString();
    byte[] contractAddress2 = PublicMethedForDailybuild
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000L, 100, null, testKeyForinternalTxsAddress,
            internalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1)
        + "\",\"" + Base58.encode58Check(contractAddress2) + "\"";
    String txid = "";
    txid = PublicMethedForDailybuild.triggerContract(contractAddress,
        "test1(address,address)", initParmes, false,
        0, maxFeeLimit, internalTxsAddress, testKeyForinternalTxsAddress, blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethedForDailybuild.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    int transactionsCount = infoById.get().getInternalTransactionsCount();
    Assert.assertEquals(4, transactionsCount);
    dupInternalTrsansactionHash(infoById.get().getInternalTransactionsList());
    for (int i = 0; i < transactionsCount; i++) {
      Assert.assertFalse(infoById.get().getInternalTransactions(i).getRejected());
    }

    String note = ByteArray
        .toStr(infoById.get().getInternalTransactions(0).getNote().toByteArray());
    String note1 = ByteArray
        .toStr(infoById.get().getInternalTransactions(1).getNote().toByteArray());
    String note2 = ByteArray
        .toStr(infoById.get().getInternalTransactions(2).getNote().toByteArray());
    String note3 = ByteArray
        .toStr(infoById.get().getInternalTransactions(3).getNote().toByteArray());
    Assert.assertEquals("call", note);
    Assert.assertEquals("create", note1);
    Assert.assertEquals("call", note2);
    Assert.assertEquals("call", note3);

    Long vaule1 = infoById.get().getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Long vaule2 = infoById.get().getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Long vaule3 = infoById.get().getInternalTransactions(2).getCallValueInfo(0).getCallValue();
    Long vaule4 = infoById.get().getInternalTransactions(3).getCallValueInfo(0).getCallValue();
    Assert.assertTrue(1 == vaule1);
    Assert.assertTrue(1000 == vaule2);
    Assert.assertTrue(0 == vaule3);
    Assert.assertTrue(1 == vaule4);


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
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


  /**
   * constructor.
   */

  public void dupInternalTrsansactionHash(
      List<org.tron.protos.Protocol.InternalTransaction> internalTransactionList) {
    List<String> hashList = new ArrayList<>();
    internalTransactionList.forEach(
        internalTransaction -> hashList
            .add(Hex.toHexString(internalTransaction.getHash().toByteArray())));
    List<String> dupHash = hashList.stream()
        .collect(Collectors.toMap(e -> e, e -> 1, (a, b) -> a + b))
        .entrySet().stream().filter(entry -> entry.getValue() > 1).map(entry -> entry.getKey())
        .collect(Collectors.toList());
    Assert.assertEquals(dupHash.size(), 0);
  }
}
