package org.tron.client;

import static org.tron.client.MainChainGatewayApi.GatewayApi.GATEWAY_API;

import com.beust.jcommander.internal.Lists;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.config.Args;
import org.tron.common.exception.RpcConnectException;
import org.tron.common.exception.TxExpiredException;
import org.tron.common.exception.TxFailException;
import org.tron.common.exception.TxRollbackException;
import org.tron.common.exception.TxValidateException;
import org.tron.common.utils.AbiUtil;
import org.tron.common.utils.WalletUtil;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.service.eventactuator.SignListParam;

@Slf4j(topic = "mainApi")
public class MainChainGatewayApi {

  // Singleton
  enum GatewayApi {
    GATEWAY_API;
    private WalletClient instance;

    GatewayApi() {
      instance = new WalletClient(Args.getInstance().getMainchainFullNode(),
          Args.getInstance().getMainchainSolidity(), Args.getInstance().getOraclePrivateKey(),
          true);
    }

    public WalletClient getInstance() {
      return instance;
    }

  }

  public static String getTRCName(String contractAddress) {
    String method = "name()";
    byte[] ret;
    try {
      ret = GATEWAY_API.getInstance()
          .triggerConstantContractAndReturn(WalletUtil.decodeFromBase58Check(contractAddress),
              method,
              Lists.newArrayList(), 0, 0, 0);
    } catch (RpcConnectException e) {
      return "default token name";
    }
    return AbiUtil.unpackString(ret);
  }

  public static String getTRCSymbol(String contractAddress) {
    String method = "symbol()";
    byte[] ret;
    try {
      ret = GATEWAY_API.getInstance()
          .triggerConstantContractAndReturn(WalletUtil.decodeFromBase58Check(contractAddress),
              method,
              Lists.newArrayList(), 0, 0, 0);
    } catch (RpcConnectException e) {
      return "default token symbol";
    }
    return AbiUtil.unpackString(ret);
  }

  public static long getTRCDecimals(String contractAddress) {
    String method = "decimals()";
    byte[] ret;
    try {
      ret = GATEWAY_API.getInstance()
          .triggerConstantContractAndReturn(WalletUtil.decodeFromBase58Check(contractAddress),
              method,
              Lists.newArrayList(), 0, 0, 0);
    } catch (RpcConnectException e) {
      return 0;
    }
    return AbiUtil.unpackUint(ret);
  }

  public static boolean getWithdrawStatus(String withdrawDataHash, String nonce)
      throws RpcConnectException {
    byte[] contractAddress = Args.getInstance().getMainchainGateway();
    String method = "withdrawDone(bytes32,uint256)";
    List params = Arrays.asList(withdrawDataHash, nonce);
    byte[] ret = GATEWAY_API.getInstance()
        .triggerConstantContractAndReturn(contractAddress, method, params, 0, 0, 0);
    return AbiUtil.unpackStatus(ret);
  }

  public static Transaction multiSignForWithdrawTRC10Transaction(String from, String tokenId,
      String value, String nonce, SignListParam signParam) throws RpcConnectException {
    byte[] contractAddress = Args.getInstance().getMainchainGateway();
    String method = "withdrawTRC10(address,trcToken,uint256,uint256,bytes[],address[])";
    List params = Arrays.asList(from, tokenId, value, nonce, signParam.getOracleSigns(),
        signParam.getOracleAddresses());
    return GATEWAY_API.getInstance()
        .triggerContractTransaction(contractAddress, method, params, 0, 0, 0);
  }

  public static Transaction multiSignForWithdrawTRC20Transaction(String from,
      String mainChainAddress, String value, String nonce, SignListParam signParam)
      throws RpcConnectException {

    byte[] contractAddress = Args.getInstance().getMainchainGateway();
    String method = "withdrawTRC20(address,address,uint256,uint256,bytes[],address[])";
    List params = Arrays.asList(from, mainChainAddress, value, nonce, signParam.getOracleSigns(),
        signParam.getOracleAddresses());
    return GATEWAY_API.getInstance()
        .triggerContractTransaction(contractAddress, method, params, 0, 0, 0);

  }

  public static Transaction multiSignForWithdrawTRC721Transaction(String from,
      String mainChainAddress, String uId, String nonce, SignListParam signParam)
      throws RpcConnectException {

    byte[] contractAddress = Args.getInstance().getMainchainGateway();
    String method = "withdrawTRC721(address,address,uint256,uint256,bytes[],address[])";
    List params = Arrays.asList(from, mainChainAddress, uId, nonce, signParam.getOracleSigns(),
        signParam.getOracleAddresses());
    return GATEWAY_API.getInstance()
        .triggerContractTransaction(contractAddress, method, params, 0, 0, 0);

  }

  public static Transaction multiSignForWithdrawTRXTransaction(String from, String value,
      String nonce, SignListParam signParam) throws RpcConnectException {

    byte[] contractAddress = Args.getInstance().getMainchainGateway();
    String method = "withdrawTRX(address,uint256,uint256,bytes[],address[])";
    List params = Arrays
        .asList(from, value, nonce, signParam.getOracleSigns(), signParam.getOracleAddresses());
    return GATEWAY_API.getInstance()
        .triggerContractTransaction(contractAddress, method, params, 0, 0, 0);
  }

  public static AssetIssueContract getAssetIssueById(String assetId) {
    return GATEWAY_API.getInstance().getAssetIssueById(assetId);
  }

  public static byte[] checkTxInfo(String txId)
      throws TxFailException, TxRollbackException {
    return GATEWAY_API.getInstance().checkTxInfo(txId);
  }

  public static boolean broadcast(Transaction transaction)
      throws RpcConnectException, TxValidateException, TxExpiredException {
    return GATEWAY_API.getInstance().broadcast(transaction);
  }
}
