package org.exoplatform.addon.wallet.fork;

@SuppressWarnings("all")
public class Keccak {

  static public class DigestKeccak extends BCMessageDigest implements Cloneable {
    public DigestKeccak(int size) {
      super(new KeccakDigest(size));
    }
  }

  static public class Digest256 extends DigestKeccak {
    public Digest256() {
      super(256);
    }
  }

}
