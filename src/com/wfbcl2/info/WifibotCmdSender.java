/*******************************************************************************
 * Copyright (c) 2012 LSIIT - Université de Strasbourg
 * Copyright (c) 2012 Erkan VALENTIN <erkan.valentin[at]unistra.fr>
 * http://www.senslab.info/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.wfbcl2.info;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.TimerTask;

public class WifibotCmdSender extends TimerTask {

	private DataOutputStream dos = null;
	private DataInputStream dis = null;
	private boolean connected = false;
	byte[] dataArray = new byte[9];
	private final int RESPONSE_LENGTH = 21;

	/**
	 * 
	 * @param socket
	 * @param dos
	 * @param dis
	 */
	public void configure(DataOutputStream dos, DataInputStream dis ) {
		this.dos = dos;
		this.dis = dis;
		this.connected = true;
	}

	/**
	 * 
	 * @param dataArray
	 * @throws IOException
	 */
	private void writeCommand(byte[] data) {

		if(!connected){	return; }

		//write request and read response
		try {
			dos.write(data);
			dos.flush();

			//read response
			byte[] rdata = new byte[RESPONSE_LENGTH];
			dis.readFully(rdata);

			/*for(int i=0;i<RESPONSE_LENGTH;i++) {
				System.out.print(String.format("%x", data[i]) + " ");
			}
			System.out.println("");*/

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param speed
	 * @throws IOException
	 */
	public void backward(int speed) {
		dataArray[0] = (byte) 0xff;		//255
		dataArray[1] = (byte) 0x07;		//size

		dataArray[2] = (byte) 0x00;		//left speed
		dataArray[3] = (byte) speed;
		dataArray[4] = (byte) 0x00;		//right speed
		dataArray[5] = (byte) speed;
		dataArray[6] = (byte) 0x0b;		//backward

		CRC16 crc = new CRC16();
		for (int i=1; i<7; i++) {
			crc.update(dataArray[i]);
		}

		dataArray[7] = (byte) crc.getValue();		//crc
		dataArray[8] = (byte) (crc.getValue()>>8);
	}


	/**
	 * 
	 * @param speed
	 * @throws IOException
	 */
	public void forward(int speed)  {

		dataArray[0] = (byte) 0xff;
		dataArray[1] = (byte) 0x07;

		dataArray[2] = (byte) 0x00;
		dataArray[3] = (byte) speed;
		dataArray[4] = (byte) 0x00;
		dataArray[5] = (byte) speed;
		dataArray[6] = (byte) 0x5b;	

		CRC16 crc = new CRC16();
		for (int i=1; i<7; i++) {
			crc.update(dataArray[i]);
		}

		dataArray[7] = (byte) crc.getValue();
		dataArray[8] = (byte) (crc.getValue()>>8);
	}


	public void nothing() {

		dataArray[0] = (byte) 0xff;
		dataArray[1] = (byte) 0x07;

		dataArray[2] = (byte) 0x00;
		dataArray[3] = (byte) 0x00;
		dataArray[4] = (byte) 0x00;
		dataArray[5] = (byte) 0x00;
		dataArray[6] = (byte) 0x5b;

		CRC16 crc = new CRC16();
		for (int i=1; i<7; i++) {
			crc.update(dataArray[i]);
		}

		dataArray[7] = (byte) crc.getValue();
		dataArray[8] = (byte) (crc.getValue()>>8);
	}

	/**
	 * 
	 * @param speed
	 * @throws IOException
	 */
	public void rotate(int speed, boolean clock)  {
		dataArray[0] = (byte) 0xff;
		dataArray[1] = (byte) 0x07;

		dataArray[2] = (byte) 0x00;
		dataArray[3] = (byte) speed;
		dataArray[4] = (byte) 0x00;
		dataArray[5] = (byte) speed;
		if(clock)
			dataArray[6] = (byte) 0x4b;
		else
			dataArray[6] = (byte) 0x1b;

		CRC16 crc = new CRC16();
		for (int i=1; i<7; i++) {
			crc.update(dataArray[i]);
		}

		dataArray[7] = (byte) crc.getValue();
		dataArray[8] = (byte) (crc.getValue()>>8);
	}


	/**
	 * 
	 * @param speed
	 * @throws IOException
	 */
	public void direction(int speed, boolean right, boolean forward)  {
		dataArray[0] = (byte) 0xff;	
		dataArray[1] = (byte) 0x07;

		if(!right)
		{
			dataArray[2] = (byte) 0x00;
			dataArray[3] = (byte) speed;

			dataArray[4] = (byte) 0x00;
			dataArray[5] = (byte) 0x00;
			if(forward)
				dataArray[6] = (byte) 0x4b;
			else
				dataArray[6] = (byte) 0x0b;
		}
		else
		{
			dataArray[2] = (byte) 0x00;
			dataArray[3] = (byte) 0x00;

			dataArray[4] = (byte) 0x00;
			dataArray[5] = (byte) speed;
			if(forward)
				dataArray[6] = (byte) 0x1b;
			else
				dataArray[6] = (byte) 0x0b;
		}


		CRC16 crc = new CRC16();
		for (int i=1; i<7; i++) {
			crc.update(dataArray[i]);
		}

		dataArray[7] = (byte) crc.getValue();
		dataArray[8] = (byte) (crc.getValue()>>8);
	}


	@Override
	public void run() {

		if(connected)
		{
			synchronized (dataArray) {
				writeCommand(dataArray);
			}
		}
	}
}