package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;


@Slf4j
public class WalletTestCommittee002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  //Witness 47.93.9.236
  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  //Witness 47.93.33.201
  private final String witnessKey002 = Configuration.getByPath("testng.conf")
      .getString("witness.key2");
  //Witness 123.56.10.6
  private final String witnessKey003 = Configuration.getByPath("testng.conf")
      .getString("witness.key3");
  //Wtiness 39.107.80.135
  private final String witnessKey004 = Configuration.getByPath("testng.conf")
      .getString("witness.key4");
  //Witness 47.93.184.2
  private final String witnessKey005 = Configuration.getByPath("testng.conf")
      .getString("witness.key5");


  private final byte[] toAddress = PublicMethedForDailybuild.getFinalAddress(testKey003);
  private final byte[] witness001Address = PublicMethedForDailybuild.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethedForDailybuild.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethedForDailybuild.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethedForDailybuild.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethedForDailybuild.getFinalAddress(witnessKey005);


  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
  }


  @Test(enabled = true)
  public void testCreateProposalMaintenanceTimeInterval() {
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(witness001Address, 10000000L,
        toAddress, testKey003, blockingStubFull));

    //0:MAINTENANCE_TIME_INTERVAL,[3*27s,24h]
    //Minimum interval
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(0L, "81000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum interval
    proposalMap.put(0L, "86400000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum -1 interval, create failed.
    proposalMap.put(0L, "80000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 interval
    proposalMap.put(0L, "86401000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(0L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003, proposalMap,
        blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateProposalAccountUpgradeCost() {
    //1:ACCOUNT_UPGRADE_COST,[0,100 000 000 000 000 000]//drop
    //Minimum AccountUpgradeCost
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(1L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum AccountUpgradeCost
    proposalMap.put(1L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 AccountUpgradeCost
    proposalMap.put(1L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 AccountUpgradeCost
    proposalMap.put(1L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(1L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));
  }

  @Test(enabled = true)
  public void testCreateProposalCreateAccountFee() {
    //2:CREATE_ACCOUNT_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum CreateAccountFee
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(2L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum CreateAccountFee
    proposalMap.put(2L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 CreateAccountFee
    proposalMap.put(2L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 CreateAccountFee
    proposalMap.put(2L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(2L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testTransactionFee() {
    //3:TRANSACTION_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum TransactionFee
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(3L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum TransactionFee
    proposalMap.put(3L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 TransactionFee
    proposalMap.put(3L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 TransactionFee
    proposalMap.put(3L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(3L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testAssetIssueFee() {
    //4:ASSET_ISSUE_FEE,[0,100 000 000 000 000 000]//drop
    //Minimum AssetIssueFee
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(4L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Duplicat proposals
    proposalMap.put(4L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum AssetIssueFee
    proposalMap.put(4L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 AssetIssueFee
    proposalMap.put(4L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 AssetIssueFee
    proposalMap.put(4L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(4L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testWitnessPayPerBlock() {
    //5:WITNESS_PAY_PER_BLOCK,[0,100 000 000 000 000 000]//drop
    //Minimum WitnessPayPerBlock
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(5L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum WitnessPayPerBlock
    proposalMap.put(5L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessPayPerBlock
    proposalMap.put(5L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessPayPerBlock
    proposalMap.put(5L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(5L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testWitnessStandbyAllowance() {
    //6:WITNESS_STANDBY_ALLOWANCE,[0,100 000 000 000 000 000]//drop
    //Minimum WitnessStandbyAllowance
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(6L, "0");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum WitnessStandbyAllowance
    proposalMap.put(6L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessStandbyAllowance
    proposalMap.put(6L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessStandbyAllowance
    proposalMap.put(6L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(6L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }

  @Test(enabled = true)
  public void testCreateNewAccountFeeInSystemControl() {
    //7:CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT,0 or 1
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(7L, "1");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum WitnessStandbyAllowance
    proposalMap.put(7L, "100000000000000000");
    Assert.assertTrue(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Minimum - 1 WitnessStandbyAllowance
    proposalMap.put(6L, "-1");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Maximum + 1 WitnessStandbyAllowance
    proposalMap.put(6L, "100000000000000001");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //Non witness account
    proposalMap.put(6L, "86400000");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(toAddress, testKey003,
        proposalMap, blockingStubFull));

  }


  @Test(enabled = true)
  public void testInvalidProposals() {
    // The index isn't from 0-9
    HashMap<Long, String> proposalMap = new HashMap<Long, String>();
    proposalMap.put(10L, "60");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));

    //The index is -1
    proposalMap.put(-1L, "6");
    Assert.assertFalse(PublicMethed.sideChainProposalCreate(witness001Address, witnessKey001,
        proposalMap, blockingStubFull));


  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


