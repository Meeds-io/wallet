package org.exoplatform.addon.wallet.fork;

import java.security.MessageDigest;

import org.bouncycastle.crypto.Digest;

@SuppressWarnings("all")
public class BCMessageDigest
                             extends MessageDigest {
  protected Digest digest;

  protected BCMessageDigest(
                            Digest digest) {
    super(digest.getAlgorithmName());

    this.digest = digest;
  }

  public void engineReset() {
    digest.reset();
  }

  public void engineUpdate(
                           byte input) {
    digest.update(input);
  }

  public void engineUpdate(
                           byte[] input,
                           int offset,
                           int len) {
    digest.update(input, offset, len);
  }

  public byte[] engineDigest() {
    byte[] digestBytes = new byte[digest.getDigestSize()];

    digest.doFinal(digestBytes, 0);

    return digestBytes;
  }
}
