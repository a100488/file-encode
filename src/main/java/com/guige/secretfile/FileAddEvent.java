package com.guige.secretfile;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
@Slf4j
public class FileAddEvent {

	/**
	 * 核心加密解密调度类
	 * 饿汉单例文件加密线程池处理模式
	 */
	private final static FileAddEvent fileAddEvent = new FileAddEvent();
	//private MyTableModel myTableModel = SecretFileUI.defaultModel;
	private final static int POOL_SIZE = 2;
	private int taskNumber = 0;
	private ExecutorService threads = null;


	public static FileAddEvent getInstance(){
		return fileAddEvent;
	}
	private FileAddEvent(){ }

	/*private int addList(File file){
		int index = myTableModel.addRow(file);
		return index;
	}*/


	/**
	 * 提交加解密的文件列表和密码进线程池，将执行加解密操作
	 * @param files
	 * @param password
	 */
	public void submit(List<File> files,String password){
		int cpuNums = Runtime.getRuntime().availableProcessors();
		threads = Executors.newFixedThreadPool(cpuNums * POOL_SIZE);
		for (File file : files) {
			CodingMethod cm = new CodingMethod(file, password);
			Future<?> retFuture = threads.submit(cm);
			//int curTaskID = this.addList(file);
			taskNumber++;
			new Timer(true).schedule(new TimerTask() {
				public void run() {
					log.info(file.getName()+"进度--->"+cm.getCurTaskByPercent());
					log.info(file.getName()+"耗时--->"+UnitFormat.getTakeTime(cm.getUsedTime()));
					//myTableModel.updateProc(curTaskID, cm.getCurTaskByPercent());
					//myTableModel.updateTime(curTaskID, UnitFormat.getTakeTime(cm.getUsedTime()));
					if(retFuture.isDone() || retFuture.isCancelled()){
						String curTaskModel = null;
						try {curTaskModel = cm.enCoding()?"加密":"解密";} catch (Exception e) {}
						log.info(file.getName()+"进度--->"+curTaskModel + (cm.isSuccess()?"完成":"失败"));
						//myTableModel.updateProc(curTaskID, curTaskModel + (cm.isSuccess()?"完成":"失败"));
						if(this.cancel()) taskNumber--;
					}
					if(taskNumber == 0){
						threads.shutdown();
					}
					System.gc();
				}
			}, 0, 50);
		}
	}

	/**
	 * 获取当前正在执行的任务数
	 * @return
	 */
	public int getTaskNumber(){
		return taskNumber;
	}
}
