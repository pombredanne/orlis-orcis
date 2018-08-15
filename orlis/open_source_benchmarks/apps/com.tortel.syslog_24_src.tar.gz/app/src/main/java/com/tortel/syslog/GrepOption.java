/* SysLog - A simple logging tool
 * Copyright (C) 2013-2016  Scott Warner <Tortel1210@gmail.com>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.tortel.syslog;

/**
 * Enum for all the grep options
 */
public enum GrepOption {
	MAIN("Main Log"),
	EVENT("Event Log"),
	KERNEL("Kernel Log"),
	LAST_KERNEL("Last Kernel Log"),
	MODEM("Modem Log"),
	ALL("All Logs");
	
	private String text;
	
	private GrepOption(String text){
		this.text = text;
	}
	
	public static GrepOption fromString(String text){
		for(GrepOption cur : GrepOption.values()){
			if(cur.text.equalsIgnoreCase(text)){
				return cur;
			}
		}
		throw new IllegalArgumentException();
	}
}
