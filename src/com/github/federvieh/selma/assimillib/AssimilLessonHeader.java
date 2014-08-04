/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.content.Context;

import com.github.federvieh.selma.assimillib.dao.AssimilLessonHeaderDataSource;

/**
 * @author frank
 *
 */
public class AssimilLessonHeader {
	private long id;
	private String name;
	private boolean starred = false;
	private String number;

	/**
	 * @param id
	 * @param name
	 * @param starred
	 */
	public AssimilLessonHeader(long id, String name, boolean starred) {
		this.id = id;
		this.name = name;
		this.starred = starred;
		this.number = name.substring(name.lastIndexOf("L"));
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

	public String getName(){
		return name;
	}

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
		AssimilLessonHeaderDataSource ds = new AssimilLessonHeaderDataSource(ctxt);
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
		AssimilLessonHeaderDataSource ds = new AssimilLessonHeaderDataSource(ctxt);
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
}
