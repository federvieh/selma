/**
 * 
 */
package com.github.federvieh.selma.assimillib;

/**
 * @author frank
 *
 */
public class AssimilLessonHeader {
	private long id;
	private String name;
	private boolean starred = false;

	public long getId(){
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}

	public String getName(){
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return
	 */
	public boolean isStarred() {
		return starred ;
	}

	/**
	 * 
	 */
	public void unstar() {
		starred = false;
	}

	/**
	 * 
	 */
	public void star() {
		starred = true;
	}
}
