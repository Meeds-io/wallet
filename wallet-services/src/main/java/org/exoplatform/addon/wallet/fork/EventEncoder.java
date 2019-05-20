package org.exoplatform.addon.wallet.fork;

import java.util.List;
import java.util.stream.Collectors;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.utils.Numeric;

/**
 * <p>
 * Ethereum filter encoding. Further limited details are available <a href=
 * "https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI#events">here</a>.
 * </p>
 */
public class EventEncoder {

  private EventEncoder() {
  }

  public static String encode(Event event) {
    String methodSignature = buildMethodSignature(event.getName(), event.getParameters());
    return buildEventSignature(methodSignature);
  }

  @SuppressWarnings("rawtypes")
  static <T extends Type> String buildMethodSignature(String methodName, List<TypeReference<T>> parameters) {

    StringBuilder result = new StringBuilder();
    result.append(methodName);
    result.append("(");
    String params = parameters.stream().map(p -> Utils.getTypeName(p)).collect(Collectors.joining(","));
    result.append(params);
    result.append(")");
    return result.toString();
  }

  public static String buildEventSignature(String methodSignature) {
    byte[] input = methodSignature.getBytes();
    byte[] hash = sha3(input, 0, input.length);
    return Numeric.toHexString(hash);
  }

  /**
   * Keccak-256 hash function.
   *
   * @param input binary encoded input data
   * @param offset of start of data
   * @param length of data
   * @return hash value
   */
  public static byte[] sha3(byte[] input, int offset, int length) {
    Keccak.DigestKeccak kecc = new Keccak.Digest256();
    kecc.update(input, offset, length);
    return kecc.digest();
  }
}
