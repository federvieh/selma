/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.content.Context;

import com.github.federvieh.selma.assimillib.AssimilDatabase.LessonType;
import com.github.federvieh.selma.assimillib.dao.AssimilLessonHeaderDataSource;

/**
 * @author frank
 *
 */
public class AssimilLessonHeader {
	private long id;
	private String lang;
	private boolean starred = false;
	private String number;
	private LessonType lessonType;

	/**
	 * @param id
	 * @param name
	 * @param starred
	 */
	public AssimilLessonHeader(long id, String lang, String number, boolean starred, LessonType lt) {
		this.id = id;
		this.starred = starred;
		this.number = number;
		this.lang = lang;
		this.lessonType = lt;
	}

	public long getId(){
		return id;
	}

//	/**
//	 * @param id
//	 */
//	public void setId(long id) {
//		this.id = id;
//	}
//	/**
//	 * @param name
//	 */
//	public void setName(String name) {
//		this.name = name;
//		this.number = name.substring(name.lastIndexOf("L"));
//	}

	/**
	 * @return
	 */
	public boolean isStarred() {
		return starred ;
	}

	/**
	 * @param ctxt 
	 * 
	 */
	public void unstar(Context ctxt) {
		starred = false;
		AssimilLessonHeaderDataSource ds = new AssimilLessonHeaderDataSource(lessonType, ctxt);
		ds.open();
		ds.unstar(this.id);
		ds.close();
	}

	/**
	 * @param ctxt 
	 * 
	 */
	public void star(Context ctxt) {
		starred = true;
		AssimilLessonHeaderDataSource ds = new AssimilLessonHeaderDataSource(lessonType, ctxt);
		ds.open();
		ds.star(this.id);
		ds.close();
	}

	/**
	 * @return
	 */
	public String getNumber() {
		return number;
	}

	/**
	 * @return
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * @return
	 */
	public LessonType getType() {
		return this.lessonType;
	}
}
