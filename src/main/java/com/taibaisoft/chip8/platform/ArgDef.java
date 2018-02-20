/*
 * Copyright (c) 2014 Jeffrey Bian
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package com.taibaisoft.chip8.platform;

public class ArgDef {
	public String name;
	public String alias;
	public String desc;
	public int required = 1;
	public int 	value; // 0 - None; 2 - Must
	public boolean dashOmmit = false;
	
	/**
	 * 
	 * @param aName The name (long) of the option.
	 * @param aAlias The short alias, usually one letter long.
	 * @param aDesc The description text for this option.
	 * @param aRequired If this parameter is required or optional.
	 * @param aValue 0 if the value of this option is optional or 2 for required.
	 */
	public ArgDef (String aName, String aAlias, String aDesc, int aRequired, int aValue) {
		this (aName, aAlias, aDesc, aRequired, aValue, false);
	}
	
	/**
	 * 
	 * @param aName The name (long) of the option.
	 * @param aAlias The short alias, usually one letter long.
	 * @param aDesc The description text for this option.
	 * @param aRequired If this parameter is required or optional.
	 * @param aValue 0 if the value of this option is optional or 2 for required.
	 * @param aDashOmmit If dashes are ommittable [Not implemented yet]
	 */
	public ArgDef (String aName, String aAlias, String aDesc, int aRequired, int aValue, boolean aDashOmmit) {
		name = aName;
		alias = aAlias;
		desc = aDesc;
		value = aValue;
		required = aRequired; 
		dashOmmit = aDashOmmit;
	}
}