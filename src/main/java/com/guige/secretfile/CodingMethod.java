package com.guige.secretfile;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
@Slf4j
public class CodingMethod implements Runnable{

	/**类成员变量
	 * curPosition	加解密的进度
	 * account		加解密的分块总数
	 * file			加密的文件
	 * password		加密的密码
	 * BYTESNUMBER	指定内存映射大小
	 */
	private int account=1;
	private int curPosition=0;
	private File file;
	private String password;
	final int BYTESNUMBER = 20971520;
	private long startTime = 0;
	private boolean isError = false;
	private boolean isSuccess = false;

	/**构造方法传入两个参数，文件和密码
	 *
	 * @param file
	 * @param password
	 */
	public CodingMethod(File file, String password) {
		this.file = file;
		this.password = password;
	}

	/**加解密主算法
	 *
	 * @param md5	用于加解密的md5码
	 * @return	处理该文件说耗费时间(Ms)
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NoSuchAlgorithmException
	 */
	public void coding(String md5) throws IOException, InterruptedException, NoSuchAlgorithmException{

		file = preDeal();
		//1、创建一个随机访问文件类   2、获取文件通道   3、创建一个内存映射文件字节缓存区域
		RandomAccessFile raf = new RandomAccessFile(file,"rwd");
		FileChannel fc = raf.getChannel();
		MappedByteBuffer buffer = null;

		//如果是解密，先去掉末8个字节
		if(enCoding()){
			raf.setLength(file.length()-8);
		}


		long fileLength = file.length();


		long volume = 0;	//volume 实际内存映射大小
		//得到分块处理数
		account = getAccount();

		//核心，加解密
		for(curPosition=0;curPosition<account;curPosition++){
			volume = fileLength/account;
			if(curPosition == account-1){
				volume+=fileLength%account;
			}
			//内存映射文件
			buffer = fc.map(FileChannel.MapMode.READ_WRITE, fileLength/account*curPosition, volume);
			//在字节的层面上对其进行异或加解密
			for(int i = 0;i<volume;i++){
				buffer.put(i, (byte) (buffer.get(i)^md5.charAt(i%32)));
			}
			//以下两句代码一起使用，先标明为垃圾，再直接回收，可以提高速度，而且大幅度降低内存的占用
			buffer.clear();
			System.gc();
		}
		//这一句一定要放在下面语句的前面，否则引发异常
		isSuccess = true;
		//如果是加密操作，末尾写入8位密钥
		if(enCoding()){
			raf.seek(fileLength);
			raf.write(getPassBytes(md5));
		}

		fc.close();
		raf.close();
	}


	/**获取Md5码
	 *
	 * @return	加密后的32位md5码
	 * @throws NoSuchAlgorithmException
	 */
	private String getMd5() throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance("md5");
		char[] hex = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		md.update(password.getBytes());
		byte[] result = md.digest();
		StringBuffer sbBuffer = new StringBuffer();
		for(int i = 0;i<result.length;i++){
			//将每个字节转为两位十六进制
			sbBuffer.append(hex[(0xff & result[i]) / 16]+""+hex[(0xff & result[i]) % 16]);
		}
		return sbBuffer.toString();
	}


	/**32位md5码加密为8位(杜绝反向解密)
	 *
	 * @param md5	32位md5码
	 * @return	加密后8个字节长的byte数组
	 */
	private byte[] getPassBytes(String md5){
		byte[] Bytes = md5.getBytes();
		byte[] md5PassBytes = new byte[8];
		for(int i=0;i<8;i++){
			md5PassBytes[i] = (byte) (Bytes[i]^Bytes[i+8]^Bytes[31-i]^Bytes[23-i]);
		}
		return md5PassBytes;
	}

	/**获取文件末八位密钥
	 *
	 * @return	8位byte数组
	 * @throws IOException
	 */
	private byte[] getPassBytes() throws IOException{
		byte[] filePassBytes = new byte[8];
		RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(file.length()-8);
		raf.read(filePassBytes);
		raf.close();
		return filePassBytes;
	}

	/**文件预处理
	 *
	 * 1、文件属性修改为可读可写
	 * 2、加密过程文件后缀加上.secret 解密过程文件后缀的.secret去除
	 *
	 * @return file 预处理后的文件
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	private File preDeal() throws IOException, NoSuchAlgorithmException{
		//确保文件可读可写
		if (!file.canRead()) {
			file.setReadable(true);
		}
		if(!file.canWrite()){
			file.setWritable(true);
		}
		//加密与解密的预更名操作
		File file2=null;
		if(enCoding()){
			file2 = new File(file+".secret");
		}
		else {
			file2 = new File(file.toString().substring(0, file.toString().lastIndexOf(".")));
		}
		//更名
		file.renameTo(file2);
		//新文件抽象类
		return file2;
	}


	/**比较两个bytes数组是否相等
	 *
	 */
	private boolean equalBytes(byte[] bytes1,byte[] bytes2){
		if(bytes1.length != bytes2.length) return false;
		for(int i=0;i<bytes1.length;i++){
			if(bytes1[i]!=bytes2[i])
				return false;
		}
		return true;
	}

	/**判断文件是否将执行加密
	 *
	 * @return	需要加密 返回true 需要解密 返回false
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 *
	 * 说明：为什么这里要这么严谨呢？直接判断后缀带不带secret不就好吗？因为经常存在一种情况，就是加密太快瞬间完成，
	 * 前台过来请求这个数据用以判断是加密还是解密的时候，读取的那个文件
	 * 已经被预处理函数修改后缀了，所以请求到的是反的。那么就需要分情况判断，如果isSuccess()为false，文件后缀带secret则是解密。isSuccess()为true，
	 * 代表文件被预处理过，后缀已经改过了，带secret应该是加密。
	 * 小细节，不容忍任何一点小bug
	 */
	public boolean enCoding() throws NoSuchAlgorithmException, IOException{
		boolean isEncoding = false;
		if(isSuccess() && file.getName().endsWith("secret")){
			isEncoding = true;
		}else{
			if(!isSuccess() && !file.getName().endsWith("secret")){
				isEncoding = true;
			}
		}
		return isEncoding;
	}

	/**判断是否有解密的权限
	 *
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	private boolean canDisCoding(){
		try {
			if(!enCoding() && equalBytes(getPassBytes(), getPassBytes(getMd5()))){
				return true;
			}
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		return false;
	}


	/**获取加解密的分块总数
	 *
	 * @return	account
	 */
	public int getAccount(){
		return (int) (file.length()/BYTESNUMBER)+1;
	}


	/**线程体
	 *
	 */
	public void run() {
		startTime = System.currentTimeMillis();
		try
		{
			if(enCoding() || canDisCoding()){
				coding(getMd5());
			}
			else{
				isError = true;
				log.error(file.getName()+"(密码错误，解密失败)");
				//JOptionPane.showMessageDialog(null, file.getName()+"(密码错误，解密失败)");
			}
		}
		catch (Exception e) {
			isError = true;
			log.error(file.getName()+"(启动线程失败)");
			//JOptionPane.showMessageDialog(null, "启动线程失败");
		}
	}

	/**获取当前加解密的实时处理位置
	 *
	 * @return	curPosition
	 */
	public int getCurPosition(){
		return curPosition;
	}

	/**
	 * 获取当前加解密百分比进度
	 * @return
	 */
	public String getCurTaskByPercent(){
		return (getCurPosition()*100)/getAccount()+"%";
	}

	/**获取任务当前已耗时间 任务出错时返回0
	 *
	 * @return
	 */
	public long getUsedTime(){
		return isError?0:System.currentTimeMillis() - startTime;
	}


	/**
	 * 本次任务是否成功
	 * @return
	 */
	public boolean isSuccess(){
		return isSuccess;
	}
}
