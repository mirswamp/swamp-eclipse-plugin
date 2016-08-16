/*
 * Copyright 2016 Malcolm Reid Jr.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eclipseplugin.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

public class DialogUtil {
	public static final String HELP_DIALOG_TITLE 	= "Help";
	public static final String CLEAR_CAPTION 		= "Clear";
	public static final String OK_CAPTION 			= "OK";
	public static final String CANCEL_CAPTION 		= "Cancel";
	public static final String BACK_CAPTION			= "Back";
	
	public static Text initializeTextWidget(int style, Composite container, GridData griddata, int span)  {
		Text text = new Text(container, style);
		griddata.horizontalSpan = span;
		text.setLayoutData(griddata);
		return text;
	}
	
	public static Text initializeTextWidget(int style, Composite container, GridData griddata) {
		return initializeTextWidget(style, container, griddata, 1);
	}
	
	public static Label initializeLabelWidget(String text, int style, Composite container, int span) {
		Label label = new Label(container, style);
		label.setText(text);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
		return label;
	}
	
	public static Label initializeLabelWidget(String text, int style, Composite container) {
		return initializeLabelWidget(text, style, container, 1);
	}
	
	public static Combo initializeComboWidget(Composite container, GridData griddata, String[] options, int span) {
		Combo combo = new Combo(container, SWT.DROP_DOWN);
		griddata.horizontalSpan = span;
		combo.setLayoutData(griddata);
		combo.setItems(options);
		return combo;
	}

	public static Combo initializeComboWidget(Composite container, GridData griddata, String[] options) {
		return initializeComboWidget(container, griddata, options, 1);
	}
	
	public static List initializeListWidget(Composite container, GridData griddata, String[] options, int span) {
		List list = new List(container, SWT.MULTI + SWT.V_SCROLL);
		griddata.horizontalSpan = span;
		list.setLayoutData(griddata);
		list.setItems(options);
		return list;
	}
	
	public static List initializeListWidget(Composite container, GridData griddata, String[] options) {
		return initializeListWidget(container, griddata, options, 1);
	}
	
	public static Button initializeButtonWidget(Composite container, String text, GridData griddata, int style, int span) {
		Button button = new Button(container, style);
		button.setText(text);
		griddata.horizontalSpan = span;
		button.setLayoutData(griddata);
		return button;
	}
	
	public static Button initializeButtonWidget(Composite container, String text, GridData griddata, int style) {
		return initializeButtonWidget(container, text, griddata, style, 1);
	}
}
