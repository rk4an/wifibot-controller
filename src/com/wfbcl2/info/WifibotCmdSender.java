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

import android.util.Log;


public class WifibotCmdSender extends TimerTask {

	private DataOutputStream dos = null;
	private DataInputStream dis = null;
	private boolean connected = false;
	byte[] dataArray = new byte[9];
	private final int RESPONSE_LENGTH = 21;
	
	private WifibotLab2Activity context = null;
	byte[] rdata = new byte[RESPONSE_LENGTH];

	public WifibotCmdSender(WifibotLab2Activity context){
		this.context = context;
	}

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
			
			//check security
			if(context.onSecurity) {
				//block forward
				if( (((int) dataArray[6] & 0x50) == 0x50) && 
						(rdata[11] > WifibotLab2Activity.IR_LIMIT || rdata[3] > WifibotLab2Activity.IR_LIMIT)){
					dataArray[2] = 0;
					dataArray[3] = 0;
					dataArray[4] = 0;
					dataArray[5] = 0;
				}
				
				if( (((int) dataArray[6] & 0x50) == 0x00) && 
						(rdata[12] > WifibotLab2Activity.IR_LIMIT || rdata[4] > WifibotLab2Activity.IR_LIMIT)){
					dataArray[2] = 0;
					dataArray[3] = 0;
					dataArray[4] = 0;
					dataArray[5] = 0;
				}
			}
			
			dos.write(data);
			dos.flush();

			String write_cmd = "";
			for(int i=0;i<9;i++) {
				write_cmd += String.format("%x", data[i]) + " ";
			}
			Log.d("WRITE",write_cmd);
			
			//read response
			dis.readFully(rdata);

			String read_cmd = "";
			for(int i=0;i<RESPONSE_LENGTH;i++) {
				read_cmd += String.format("%x", rdata[i]) + " ";
			}
			Log.d("READ",read_cmd);
			
			context.voltage = (short)(rdata[2] & 0xff);
			context.irFr = (short)(rdata[11] & 0xff);
			context.irFl = (short)(rdata[3] & 0xff);
			context.irBr = (short)(rdata[4] & 0xff);
			context.irBl = (short)(rdata[12] & 0xff);
			context.current = (short)(rdata[17] & 0xff);
			context.batteryState = (short)(rdata[18] & 0xff);
			context.handler.post(context.updateUI);
			

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

		dataArray[2] = (byte) speed;	//left speed
		dataArray[3] = (byte) (speed>>8);
		dataArray[4] = (byte) speed;	//right speed
		dataArray[5] = (byte) (speed>>8);
		dataArray[6] = (byte) 0x03;		//backward
		
		if(context.onMotorControl) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0xa0);
		}
		
		if(WifibotLab2Activity.MOTOR_CONTROL_PID_10) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0x08);
		}

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

		dataArray[2] = (byte) speed;
		dataArray[3] = (byte) (speed>>8);
		dataArray[4] = (byte) speed;
		dataArray[5] = (byte) (speed>>8);
		dataArray[6] = (byte) 0x53;

		if(context.onMotorControl) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0xa0);
		}

		if(WifibotLab2Activity.MOTOR_CONTROL_PID_10) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0x08);
		}
		
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
		dataArray[6] = (byte) 0x53;

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

		dataArray[2] = (byte) speed;
		dataArray[3] = (byte) (speed>>8);
		dataArray[4] = (byte) speed;
		dataArray[5] = (byte) (speed>>8);
		if(clock) {
			dataArray[6] = (byte) 0x43;
		}
		else {
			dataArray[6] = (byte) 0x13;
		}

		if(context.onMotorControl) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0xa0);
		}
		
		if(WifibotLab2Activity.MOTOR_CONTROL_PID_10) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0x08);
		}
		
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
			dataArray[2] = (byte) speed;
			dataArray[3] = (byte) (speed>>8);

			dataArray[4] = (byte) 0x00;
			dataArray[5] = (byte) 0x00;
			if(forward) {
				dataArray[6] = (byte) 0x43;
			}
			else {
				dataArray[6] = (byte) 0x03;
			}
		}
		else
		{
			dataArray[2] = (byte) 0x00;
			dataArray[3] = (byte) 0x00;

			dataArray[4] = (byte) speed;
			dataArray[5] = (byte) (speed>>8);
			if(forward) {
				dataArray[6] = (byte) 0x13;
			}
			else {
				dataArray[6] = (byte) 0x03;
			}
		}

		if(context.onMotorControl) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0xa0);
		}

		if(WifibotLab2Activity.MOTOR_CONTROL_PID_10) {
			dataArray[6] = (byte) ((int)dataArray[6] | 0x08);
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