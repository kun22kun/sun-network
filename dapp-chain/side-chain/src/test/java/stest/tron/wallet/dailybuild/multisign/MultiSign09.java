package stest.tron.wallet.dailybuild.multisign;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethedForDailybuild;
import stest.tron.wallet.common.client.utils.PublicMethedForMutiSign;

@Slf4j
public class MultiSign09 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethedForDailybuild.getFinalAddress(testKey002);

  private final String witnessKey001 = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress001 = PublicMethedForDailybuild.getFinalAddress(witnessKey001);

  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] ownerAddress = ecKey1.getAddress();
  private String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] normalAddr001 = ecKey2.getAddress();
  private String normalKey001 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  private ECKey tmpEcKey01 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr01 = tmpEcKey01.getAddress();
  private String tmpKey01 = ByteArray.toHexString(tmpEcKey01.getPrivKeyBytes());

  private ECKey tmpEcKey02 = new ECKey(Utils.getRandom());
  private byte[] tmpAddr02 = tmpEcKey02.getAddress();
  private String tmpKey02 = ByteArray.toHexString(tmpEcKey02.getPrivKeyBytes());

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");


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
  }

  @Test(enabled = true, description = "Active doesn't have parent_id")
  public void testActiveParent01() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02)
            + "\",\"weight\":3}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\","
            + "\"threshold\":4,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2,
        PublicMethedForMutiSign.getActivePermissionKeyCount(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));
    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);

    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = true, description = "Active parent_id is exception condition")
  public void testActiveParent02() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(ownerAddress, 1_000_000, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    // parent_id = "123"
    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":\"123\",\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    GrpcAPI.Return response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = 123
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":123,\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = "abc"
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":\"abc\",\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    boolean ret = false;
    try {
      PublicMethedForDailybuild
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NumberFormatException e) {
      logger.info("Expected NumberFormatException!");
      ret = true;
    }
    Assert.assertTrue(ret);

    // parent_id = ""
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":\"\",\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    ret = false;
    try {
      PublicMethedForDailybuild
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);

    // parent_id = null
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":" + null
            + ",\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    ret = false;
    try {
      PublicMethedForDailybuild
          .accountPermissionUpdateForResponse(accountPermissionJson,
              ownerAddress, ownerKey, blockingStubFull);
    } catch (NullPointerException e) {
      logger.info("Expected NullPointerException!");
      ret = true;
    }
    Assert.assertTrue(ret);

    // parent_id = 1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":1,\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = 2
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":2,\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = 3
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":3,\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = -1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":-1,\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = Integer.MAX_VALUE
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":2147483647,\"type\":2,"
            + "\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = Integer.MAX_VALUE +1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":2147483648,\"type\":2,"
            + "\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = Integer.MIN_VALUE
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":-2147483648,\"type\":2,"
            + "\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());

    // parent_id = Integer.MIN_VALUE -1
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":-2147483649,\"type\":2,"
            + "\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";

    response = PublicMethedForDailybuild.accountPermissionUpdateForResponse(
        accountPermissionJson, ownerAddress, ownerKey, blockingStubFull);
    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : permission's parent should be owner",
        response.getMessage().toStringUtf8());
    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }

  @Test(enabled = true, description = "Active parent_id is 0")
  public void testActiveParent03() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    final byte[] ownerAddress = ecKey1.getAddress();
    final String ownerKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    long needCoin = updateAccountPermissionFee * 2;

    Assert.assertTrue(PublicMethedForDailybuild.sendcoin(ownerAddress, needCoin, fromAddress,
        testKey002, blockingStubFull));
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);
    Long balanceBefore = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    List<String> ownerPermissionKeys = new ArrayList<>();

    PublicMethedForDailybuild.printAddress(ownerKey);
    PublicMethedForDailybuild.printAddress(tmpKey02);

    ownerPermissionKeys.add(ownerKey);

    String accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":3}]},"
            + "\"active_permissions\":["
            + "{\"parent_id\":0,\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(witnessKey001) + "\",\"weight\":3},"
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(tmpKey02) + "\",\"weight\":1}"
            + "]}]}";
    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    PublicMethedForDailybuild.waitProduceNextBlock(blockingStubFull);

    ownerPermissionKeys.clear();
    ownerPermissionKeys.add(tmpKey02);

    Assert.assertEquals(2, PublicMethedForMutiSign.getActivePermissionKeyCount(
        PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getActivePermissionList()));

    Assert.assertEquals(1, PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getOwnerPermission().getKeysCount());

    PublicMethedForMutiSign.printPermissionList(PublicMethedForDailybuild.queryAccount(ownerAddress,
        blockingStubFull).getActivePermissionList());

    System.out
        .printf(PublicMethedForMutiSign.printPermission(PublicMethedForDailybuild.queryAccount(ownerAddress,
            blockingStubFull).getOwnerPermission()));

    logger.info("** trigger a permission transaction");
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":1,\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":1,"
            + "\"operations\":\"3f3d1ec0032001000000000000000000000000000000000000000000000000c0\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethedForDailybuild.getAddressString(ownerKey) + "\",\"weight\":1}"
            + "]}]}";

    Assert.assertTrue(PublicMethedForMutiSign.accountPermissionUpdate(accountPermissionJson,
        ownerAddress, ownerKey, blockingStubFull,
        ownerPermissionKeys.toArray(new String[ownerPermissionKeys.size()])));

    Long balanceAfter = PublicMethedForDailybuild.queryAccount(ownerAddress, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
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
