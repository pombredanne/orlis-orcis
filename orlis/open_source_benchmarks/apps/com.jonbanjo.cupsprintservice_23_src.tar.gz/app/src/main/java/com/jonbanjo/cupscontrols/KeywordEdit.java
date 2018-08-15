package com.jonbanjo.cupscontrols;

/*
JfCupsPrintService
Copyright (C) 2014 Jon Freeman

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import com.jonbanjo.cups.ppd.PpdItem;
import com.jonbanjo.cups.ppd.PpdItemList;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class KeywordEdit extends Spinner{

	private PpdItemList section;
	
	public KeywordEdit(Context context){
		super(context);
	}
	
	public KeywordEdit(int id, Context context, int resId, PpdItemList section){
		super(context);
	 	this.section = section;
	 	setId(id);
	 	ArrayAdapter <PpdItem> aa = 
				new ArrayAdapter<PpdItem>(getContext(), resId, section);
	 			//new ArrayAdapter<PpdItem>(getContext(),android.R.layout.simple_spinner_item, section);
		setAdapter(aa);
		int size = section.size();
		for (int i=0; i<size; i++){
			if (section.get(i).getValue().equals(section.getSavedValue())){
				this.setSelection(i);
				break;
			}
		
		}
	 }
	
	 public boolean validate(){
		 return true;
	 }
	 
	 public void update(){
	 	PpdItem item = (PpdItem) this.getSelectedItem();
	 	section.setSavedValue(item.getValue());
	 }
}
