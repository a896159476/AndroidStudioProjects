package com.example.administrator.myapplication;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
* @author jamin
* @date 2016/9/7
* @desc  统一管理activity
*/
public final class ActivityUtil {

	private ActivityUtil(){}

	public static List<Activity> activities = new ArrayList<Activity>();

	/**
	 * 
	 * @Title: addActivity
	 * @Description: 向list中添加活动
	 * @param @param activity 加入list中的活动
	 * @return void 返回类型
	 * @throws
	 */
	public static void addActivity(Activity activity) {
		activities.add(activity);
	}

	/**
	 * 
	 * @Title: removeActivity
	 * @Description: 向list中删除活动
	 * @param @param activity 从list中删除的活动
	 * @return void 返回类型
	 * @throws
	 */
	public static void removeActivity(Activity activity) {
		activities.remove(activity);
	}

	/**
	 * 
	 * @Title: finishAll
	 * @Description: 关闭所有活动
	 * @return void 返回类型
	 * @throws
	 */
	public static void finishAll() {
		for (Activity activity : activities) {
			if (!activity.isFinishing()) {
				activity.finish();
			}
		}
	}
}
