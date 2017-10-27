package com.guige.secretfile;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**拖放文件监听类
 *
 * @author Administrator
 *
 */
@Slf4j
public class DropFile{
	/**
	 *
	 * @param args  urls=   pwd=
	 * @throws Exception
	 */
	public static void main(String[] args)throws  Exception {

		/*List<String> fileList = new ArrayList<>();
		fileList.add("D:\\guige\\OIMDS");
		drop(fileList,"ddd");*/

		List<String> fileList = new ArrayList<>();
		if (args != null&&args.length==2) {
			System.out.println("args.length::" + args.length);
			String urlStr = args[0];
			if(urlStr.contains("url=")){
				urlStr=urlStr.replace("url=","");
				String[] urls= urlStr.split(",");
				if(urls.length<1){
					System.out.println("加密or解密文件不能为空");
					return;
				}
				fileList.addAll(Arrays.asList(urls));
			}else{
				System.out.println("加密or解密文件不能为空");
				return;
			}

			String	password =args[1];
			if(password.contains("pwd=")){
				password=password.replace("pwd=","");
				if(password.length()<1){
					System.out.println("请输入密码(忘记密码将永久无法解密)");
					return;
				}
				drop(fileList,password);
			}else{
				System.out.println("请输入密码(忘记密码将永久无法解密)");
				return;
			}

		} else {
			System.out.println("加密or解密文件不能为空");
		}
		//String	password = JOptionPane.showInputDialog(null, "请输入密码(忘记密码将永久无法解密)：","密码确认",JOptionPane.QUESTION_MESSAGE);
	}

	public static void drop(List<String> fileList,String password) {

			try {
				//获取文件List
				List<File> files = new ArrayList<>();
				//遍历文件List
				for(Object fileString : fileList){
					File file = new File(fileString.toString());
					//过滤掉文件夹
					if(file.isFile()){
						files.add(file);
					}else{
						files.addAll(searchFiles(file.listFiles()));
					}
				}
				FileAddEvent.getInstance().submit(files, password);

			} catch ( Exception e1) {
				log.error("接受文件失败",e1);

			}

	}
	private static List<File> searchFiles(File[] fm) {
		// 查找目录

		// 要查找的关键字
		List<File> fms = new ArrayList<>();
		for (File file : fm) {

			if (file.isDirectory()) {
				fms.addAll(searchFiles(file.listFiles()));

			} else {
				fms.add(file);
			}
		}
		return fms;
	}


	/**
	 * 删除单个文件
	 *
	 * @param fileName
	 *            要删除的文件的文件名
	 * @return 单个文件删除成功返回true，否则返回false
	 */
	public static boolean deleteFile(String fileName) {
		File file = new File(fileName);
		// 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
		if (file.exists() && file.isFile()) {
			if (file.delete()) {
				System.out.println("删除单个文件" + fileName + "成功！");
				return true;
			} else {
				System.out.println("删除单个文件" + fileName + "失败！");
				return false;
			}
		} else {
			System.out.println("删除单个文件失败：" + fileName + "不存在！");
			return false;
		}
	}
	/**
	 * 删除目录及目录下的文件
	 *
	 * @param dir
	 *            要删除的目录的文件路径
	 * @return 目录删除成功返回true，否则返回false
	 */
	public static boolean deleteDirectory(String dir) {
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		if (!dir.endsWith(File.separator))
			dir = dir + File.separator;
		File dirFile = new File(dir);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
			System.out.println("删除目录失败：" + dir + "不存在！");
			return false;
		}
		boolean flag = true;
		// 删除文件夹中的所有文件包括子目录
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag)
					break;
			}
			// 删除子目录
			else if (files[i].isDirectory()) {
				flag = deleteDirectory(files[i]
						.getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag) {
			System.out.println("删除目录失败！");
			return false;
		}
		// 删除当前目录
		if (dirFile.delete()) {
			System.out.println("删除目录" + dir + "成功！");
			return true;
		} else {
			return false;
		}
	}
}
