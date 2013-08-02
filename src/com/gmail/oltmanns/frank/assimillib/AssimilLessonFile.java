/**
 * 
 */
package com.gmail.oltmanns.frank.assimillib;

/**
 * @author frank
 *
 */
public class AssimilLessonFile {

	private String text;
	private String id;

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	public AssimilLessonFile(String text, String id) {
		this.text = text;
		this.id = id;
	}

}
