/* NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.macastiblancot.lectorsitp.sinpolib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.nfc.tech.IsoDep;

public class Iso7816 {
	public static final byte[] EMPTY = { 0 };

	protected byte[] data;

	protected Iso7816(byte[] bytes) {
		data = (bytes == null) ? Iso7816.EMPTY : bytes;
	}

	public boolean match(byte[] bytes, int start) {
		final byte[] data = this.data;
		if (data.length <= bytes.length - start) {
			for (final byte v : data) {
				if (v != bytes[start++])
					return false;
			}
		} else {
			return false;
		}
		return true;
	}

	public byte[] getBytes() {
		return data;
	}

	@Override
	public String toString() {
		return Util.toHexString(data, 0, data.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null || !(obj instanceof Iso7816))
			return false;

		return match(((Iso7816) obj).getBytes(), 0);
	}

	public static class Response extends Iso7816 {
		public static final byte[] EMPTY = {};
		public static final byte[] ERROR = { 0x6F, 0x00 }; // SW_UNKNOWN

		public Response(byte[] bytes) {
			super((bytes == null || bytes.length < 2) ? Response.ERROR : bytes);
		}

		public short getSw12() {
			final byte[] d = this.data;
			int n = d.length;
			return (short) ((d[n - 2] << 8) | (0xFF & d[n - 1]));
		}

		public boolean isOkay() {
			return equalsSw12(SW_NO_ERROR);
		}

		public boolean equalsSw12(short val) {
			return getSw12() == val;
		}

		public int size() {
			return data.length - 2;
		}

		public byte[] getBytes() {
			return isOkay() ? Arrays.copyOfRange(data, 0, size())
					: Response.EMPTY;
		}
	}

	public final static class StdTag {
		private final IsoDep nfcTag;

		public StdTag(IsoDep tag) {
			nfcTag = tag;
		}

        public Response selectSITP() throws IOException{
            return selectByName(new byte[]{(byte)-0x2c, (byte)0x10, (byte)0x00, (byte)0x00,
                    (byte)0x03, (byte)0x00, (byte)0x01});
        }

        public int getBalance() throws IOException{
            return Util.toInt(getBalanceResponse().getBytes(), 0, 4);
        }

		private Response getBalanceResponse() throws IOException{
			return new Iso7816.Response(transceive(new byte[]{(byte)-0x70, (byte)0x4C, (byte)0x00,
                    (byte)0x00, (byte)0x04}));
		}

		private Response selectByName(byte... name) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(name.length + 6);
			buff.put((byte) 0x00) // CLA Class
					.put((byte) 0xA4) // INS Instruction
					.put((byte) 0x04) // P1 Parameter 1
					.put((byte) 0x00) // P2 Parameter 2
					.put((byte) name.length) // Lc
					.put(name).put((byte) 0x00); // Le

			return new Response(transceive(buff.array()));
		}


		private byte[] transceive(final byte[] cmd) throws IOException {
			try {
				byte[] rsp = null;

				byte c[] = cmd;
				do {
					byte[] r = nfcTag.transceive(c);
					if (r == null)
						break;

					int N = r.length - 2;
					if (N < 0) {
						rsp = r;
						break;
					}

					if (r[N] == CH_STA_LE) {
						c[c.length - 1] = r[N + 1];
						continue;
					}

					if (rsp == null) {
						rsp = r;
					} else {
						int n = rsp.length;
						N += n;

						rsp = Arrays.copyOf(rsp, N);

						n -= 2;
						for (byte i : r)
							rsp[n++] = i;
					}

					if (r[N] != CH_STA_MORE)
						break;

					byte s = r[N + 1];
					if (s != 0) {
						c = CMD_GETRESPONSE.clone();
					} else {
						rsp[rsp.length - 1] = CH_STA_OK;
						break;
					}

				} while (true);

				return rsp;

			} catch (Exception e) {
				return Response.ERROR;
			}
		}

		private static final byte CH_STA_OK = (byte) 0x90;
		private static final byte CH_STA_MORE = (byte) 0x61;
		private static final byte CH_STA_LE = (byte) 0x6C;
		private static final byte CMD_GETRESPONSE[] = { 0, (byte) 0xC0, 0, 0,
				0, };
	}

	public static final short SW_NO_ERROR = (short) 0x9000;
}
