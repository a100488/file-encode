package com.guige.secretfile;

import java.io.File;


/**
 * 原创算法 - 单位格式转制工具类
 * @author CMX QQ946800151
 *
 */
public class UnitFormat {

	/**
	 * 将byte转换为相应级别的单位量
	 * @param file
	 * @return
	 */
	public static String getFileLength(File file){
		float fileLength = file.length();
		String[] units = {"Byte","KB","MB","GB","TB","PB"};
		int cur_unit = 0;
		while(fileLength >= 1024){
			fileLength /= 1024;
			cur_unit++;
		}
		return (float)Math.round(fileLength*100)/100 + units[cur_unit];
	}

	/**
	 * 将毫秒转换为带单位的时间字符串 形式：1d3h20m3s256ms 一天3小时20分3秒256毫秒
	 * 最多只能保有两个单位，避免字符串过长:1d3h20m3s256ms将被截取成1d3h，其后的被舍弃。3h20m3s256ms->3h20m	20m3s256ms->20m3s
	 * @param millis
	 * @return
	 */
	public static String getTakeTime(long millis){
		String[] units = {"ms","s","m","h","d"};
		StringBuffer taskTime = new StringBuffer();
		boolean stop = false;
		long seconds = 0;
		long minutes = 0;
		long hours = 0;
		long days = 0;
		/**
		 * 此段为最高效率算法模型
		 */
		while(true){
			if(millis >= 1000){
				seconds = millis/1000;
				millis %= 1000;
				if(seconds >= 60){
					minutes = seconds/60;
					seconds %= 60;
					if(minutes >= 60){
						hours = minutes/60;
						minutes %= 60;
						if(hours >= 24){
							days = hours/24;
							hours %= 24;
							taskTime.append(days+units[4]);
							stop = true;
						}
						taskTime.append(hours+units[3]);
						if(stop){break;}
						stop = true;
					}
					taskTime.append(minutes+units[2]);
					if(stop){break;}
					stop = true;
				}
				taskTime.append(seconds+units[1]);
				if(stop){break;}
				stop = true;
			}
			taskTime.append(millis+units[0]);
			break;
		}
		return taskTime.toString();
	}
}
