package org.astri.abe.cpabe;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.astri.abe.bswabe.Bswabe;
import org.astri.abe.bswabe.BswabeCph;
import org.astri.abe.bswabe.BswabeCphKey;
import org.astri.abe.bswabe.BswabeElementBoolean;
import org.astri.abe.bswabe.BswabeMsk;
import org.astri.abe.bswabe.BswabePrv;
import org.astri.abe.bswabe.BswabePub;
import org.astri.abe.bswabe.SerializeUtils;
import org.astri.abe.cpabe.policy.LangPolicy;

import it.unisa.dia.gas.jpbc.Element;

public class Cpabe {

	/**
	 * @param args
	 * @author Junwei Wang(wakemecn@gmail.com)
	 */

	public void setup(String pubfile, String mskfile) throws IOException,
			ClassNotFoundException {
		byte[] pub_byte, msk_byte;
		BswabePub pub = new BswabePub();
		BswabeMsk msk = new BswabeMsk();
		Bswabe.setup(pub, msk);

		/* store BswabePub into mskfile */
		pub_byte = SerializeUtils.serializeBswabePub(pub);
		Common.spitFile(pubfile, pub_byte);

		/* store BswabeMsk into mskfile */
		msk_byte = SerializeUtils.serializeBswabeMsk(msk);
		Common.spitFile(mskfile, msk_byte);
	}

	public void keygen(String pubfile, String prvfile, String mskfile,
			String[] attributeArray) throws NoSuchAlgorithmException, IOException {
		BswabePub pub;
		BswabeMsk msk;
		byte[] pub_byte, msk_byte, prv_byte;

		/* get BswabePub from pubfile */
		pub_byte = Common.suckFile(pubfile);
		pub = SerializeUtils.unserializeBswabePub(pub_byte);

		/* get BswabeMsk from mskfile */
		msk_byte = Common.suckFile(mskfile);
		msk = SerializeUtils.unserializeBswabeMsk(pub, msk_byte);

		BswabePrv prv = Bswabe.keygen(pub, msk, attributeArray);

		/* store BswabePrv into prvfile */
		prv_byte = SerializeUtils.serializeBswabePrv(prv);
		Common.spitFile(prvfile, prv_byte);
	}

	public void enc(String pubfile, String policy, String inputfile,
			String encfile) throws Exception {
		BswabePub pub;
		BswabeCph cph;
		BswabeCphKey keyCph;
		byte[] plt;
		byte[] cphBuf;
		byte[] aesBuf;
		byte[] pub_byte;
		Element m;

		/* get BswabePub from pubfile */
		pub_byte = Common.suckFile(pubfile);
		pub = SerializeUtils.unserializeBswabePub(pub_byte);

		keyCph = Bswabe.enc(pub, policy);
		cph = keyCph.cph;
		m = keyCph.key;
		// System.err.println("m = " + m.toString());

		if (cph == null) {
			System.out.println("Error happed in enc");
			throw new Exception("Error in encryption");
		}

		cphBuf = SerializeUtils.bswabeCphSerialize(cph);

		/* read file to encrypted */
		plt = Common.suckFile(inputfile);
		aesBuf = AESCoder.encrypt(m.toBytes(), plt);
		// PrintArr("element: ", m.toBytes());
		Common.writeCpabeFile(encfile, cphBuf, aesBuf);
	}

	public boolean dec(String pubfile, String prvfile, String encfile,
			String decfile) throws Exception {
		byte[] aesBuf, cphBuf;
		byte[] plt;
		byte[] prv_byte;
		byte[] pub_byte;
		byte[][] tmp;
		BswabeCph cph;
		BswabePrv prv;
		BswabePub pub;

		/* get BswabePub from pubfile */
		pub_byte = Common.suckFile(pubfile);
		pub = SerializeUtils.unserializeBswabePub(pub_byte);

		/* read ciphertext */
		tmp = Common.readCpabeFile(encfile);
		aesBuf = tmp[0];
		cphBuf = tmp[1];
		cph = SerializeUtils.bswabeCphUnserialize(pub, cphBuf);

		/* get BswabePrv form prvfile */
		prv_byte = Common.suckFile(prvfile);
		prv = SerializeUtils.unserializeBswabePrv(pub, prv_byte);

		BswabeElementBoolean beb = Bswabe.dec(pub, prv, cph);
		// System.err.println("e = " + beb.e.toString());
		if (beb.b) {
			plt = AESCoder.decrypt(beb.e.toBytes(), aesBuf);
			Common.spitFile(decfile, plt);
			return true;
		} else {
			return false;
		}
	}

}
