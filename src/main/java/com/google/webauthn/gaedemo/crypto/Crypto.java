/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.webauthn.gaedemo.crypto;

import com.google.common.primitives.Bytes;
import com.google.webauthn.gaedemo.exceptions.WebAuthnException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

public class Crypto {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static byte[] sha256Digest(byte[] input) {
    SHA256Digest digest = new SHA256Digest();
    digest.update(input, 0, input.length);
    byte[] result = new byte[digest.getDigestSize()];
    digest.doFinal(result, 0);
    return result;
  }

  public static byte[] digest(byte[] input, String alg) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(alg);
    return digest.digest(input);
  }

  public static boolean verifySignature(PublicKey publicKey, byte[] signedBytes, byte[] signature)
      throws WebAuthnException {
    try {
      Signature ecdsaSignature = Signature.getInstance("SHA256withECDSA");
      ecdsaSignature.initVerify(publicKey);
      ecdsaSignature.update(signedBytes);
      return ecdsaSignature.verify(signature);
    } catch (InvalidKeyException e) {
      throw new WebAuthnException("Error when verifying signature", e);
    } catch (SignatureException e) {
      throw new WebAuthnException("Error when verifying signature", e);
    } catch (NoSuchAlgorithmException e) {
      throw new WebAuthnException("Error when verifying signature", e);
    }
  }

  // TODO add test for this.
  public static boolean verifySignature(X509Certificate attestationCertificate, byte[] signedBytes, byte[] signature)
      throws WebAuthnException {
    return verifySignature(attestationCertificate.getPublicKey(), signedBytes, signature);
  }

  public static PublicKey decodePublicKey(byte[] x, byte[] y) throws WebAuthnException {
    try {
      X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
      ECPoint point;
      try {
        byte[] encodedPublicKey = Bytes.concat(new byte[]{0x04}, x, y);
        point = curve.getCurve().decodePoint(encodedPublicKey);
      } catch (RuntimeException e) {
        throw new WebAuthnException("Couldn't parse user public key", e);
      }

      return KeyFactory.getInstance("ECDSA").generatePublic(new ECPublicKeySpec(point,
          new ECParameterSpec(curve.getCurve(), curve.getG(), curve.getN(), curve.getH())));
    } catch (InvalidKeySpecException e) {
      throw new WebAuthnException("Error when decoding public key", e);
    } catch (NoSuchAlgorithmException e) {
      throw new WebAuthnException("Error when decoding public key", e);
    }
  }
}
