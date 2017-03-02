/*
 * Copyright 2016-2017 Malcolm Reid Jr.
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

package org.continuousassurance.swamp.eclipse.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

/**
 * This class provides utility methods for performing common operations that
 * dialogs perform
 * @author Malcolm Reid Jr. (reid-jr@cs.wisc.edu)
 * @since 07/2016 
 */
public class DialogUtil {
	/**
	 * Title of the help dialog that pops up when a user seeks help with a
	 * dialog's widget
	 */
	public static final String HELP_DIALOG_TITLE 	= "Help";
	/**
	 * Caption for "Clear" button (clears all widgets on dialog)
	 */
	public static final String CLEAR_CAPTION 		= "Clear";
	/**
	 * Caption for "OK" button (advances to next dialog)
	 */
	public static final String OK_CAPTION 			= "OK";
	/**
	 * Caption for "Cancel" button (cancels submission process)
	 */
	public static final String CANCEL_CAPTION 		= "Cancel";
	/**
	 * Caption for "Back" button (returns to previous dialog)
	 */
	public static final String BACK_CAPTION			= "Back";
	
	/**
	 * Creates a Text widget on the passed in Composite and initializes it
	 * according to the specified parameters
	 *
	 * @param style the style for the widget (see Eclipse SWT Text API for more
	 * details)
	 * @param container the Composite that the widget will be placed on
	 * @param griddata the GridData object that specifies the widget's layout
	 * (Note: There should be a unique GridData object for each widget)
	 * @param span the span in columns of the widget
	 * @return Text control
	 */
	public static Text initializeTextWidget(int style, Composite container, GridData griddata, int span)  {
		Text text = new Text(container, style);
		griddata.horizontalSpan = span;
		text.setLayoutData(griddata);
		return text;
	}
	
	/**
	 * Creates a Text widget on the passed in Composite, initializes it 
	 * according to the specified parameters, and gives it a span of 1 column
	 * This is equivalent to calling initializeTextWidget(style, container, griddata, 1)
	 *
	 * @param style the style for the widget (see Eclipse Text API for more
	 * details)
	 * @param container the Composite that the widget will be placed on
	 * @param griddata the GridData object that specifies the widget's layout
	 * (Note: There should be a unique GridData object for each widget)
	 * @return Text control
	 */
	public static Text initializeTextWidget(int style, Composite container, GridData griddata) {
		return initializeTextWidget(style, container, griddata, 1);
	}
	
	/**
	 * Creates a Label widget on the passed in Composite and initializes it
	 * according to the specified parameters
	 *
	 * @param text the caption for the label widget
	 * @param style the style for the widget (see Eclipse Text API for more
	 * details)
	 * @param container the Composite that the widget will be placed on
	 * @param span the span in columns of the widget
	 * @return Label control
	 */
	public static Label initializeLabelWidget(String text, int style, Composite container, int span) {
		Label label = new Label(container, style);
		label.setText(text);
		GridData gd = new GridData(SWT.FILL, SWT.NONE, false, false);
		gd.horizontalSpan = span;
		label.setLayoutData(gd);
		return label;
	}
	
	/**
	 * Creates a Label widget on the passed in Composite, initializes it
	 * according to the specified parameters, and gives it a span of 1 column
	 * This is equivalent to calling initializeLabelWidget(text, style, container, 1)
	 *
	 * @param text the caption for the label widget
	 * @param style the style for the widget (see Eclipse Text API for more
	 * details)
	 * @param container the Composite that the widget will be placed on
	 * @return Label control
	 */
	public static Label initializeLabelWidget(String text, int style, Composite container) {
		return initializeLabelWidget(text, style, container, 1);
	}
	
	/**
	 * Creates a dropdown Combo widget on the passed in Composite and 
	 * initializes it according to the specified parameters
	 *
	 * @param container the Composite that the widget will be placed on
	 * @param griddata the GridData object that specifies the widget's layout
	 * @param options list of options to choose from in the Combo
	 * @param span the span in columns of the widget
	 * @return dropdown Combo control
	 */
	public static Combo initializeComboWidget(Composite container, GridData griddata, String[] options, int span) {
		Combo combo = new Combo(container, SWT.DROP_DOWN);
		griddata.horizontalSpan = span;
		combo.setLayoutData(griddata);
		combo.setItems(options);
		return combo;
	}

	/**
	 * Creates a dropdown Combo widget on the passed in Composite, initializes
	 * it according to the specified parameters, and gives it a span of 1 column
	 * This is equivalent to calling initializeComboWidget(container, griddata, options, 1)
	 *
	 * @param container the Composite that the widget will be placed on
	 * @param griddata the GridData object that specifies the widget's layout
	 * @param options list of options to choose from in the Combo
	 * @return dropdown Combo control
	 */
	public static Combo initializeComboWidget(Composite container, GridData griddata, String[] options) {
		return initializeComboWidget(container, griddata, options, 1);
	}
	
	/**
	 * Creates a multi-select List widget on the passed in Composite and 
	 * initializes it according to the specified parameters
	 *
	 * @param container the Composite that the widget will be placed on
	 * @param griddata the GridData object that specifies the widget's layout
	 * @param options list of options to choose from in the Combo
	 * @param span the span in columns of the widget
	 * @return multi-select List control
	 */
	public static List initializeListWidget(Composite container, GridData griddata, String[] options, int span) {
		List list = new List(container, SWT.MULTI + SWT.V_SCROLL);
		griddata.horizontalSpan = span;
		list.setLayoutData(griddata);
		list.setItems(options);
		return list;
	}
	
	/**
	 * Creates a multi-select List widget on the passed in Composite, 
	 * initializes it according to the specified parameters, and gives it a span 
	 * of 1 column
	 * This is equivalent to calling initializeListWidget(container, griddata, options, 1)
	 *
	 * @param container the Composite that the widget will be placed on
	 * @param griddata the GridData object that specifies the widget's layout
	 * @param options list of options to choose from in the Combo
	 * @param span the span in columns of the widget
	 * @return multi-select List control
	 */
	public static List initializeListWidget(Composite container, GridData griddata, String[] options) {
		return initializeListWidget(container, griddata, options, 1);
	}
	
	/**
	 * Creates a Button widget on the passed in Composite and initializes it 
	 * according to the specified parameters
	 *
	 * @param container the Composite that the widget will be placed on
	 * @param text the caption on the button
	 * @param griddata the GridData object that specifies the widget's layout
	 * @param style the style for the button (see Eclipse SWT Button API for
	 * more details)
	 * @param span the span in columns of the widget
	 * @return Button control
	 */
	public static Button initializeButtonWidget(Composite container, String text, GridData griddata, int style, int span) {
		Button button = new Button(container, style);
		button.setText(text);
		griddata.horizontalSpan = span;
		button.setLayoutData(griddata);
		return button;
	}
	
	/**
	 * Creates a Button widget on the passed in Composite, initializes it 
	 * according to the specified parameters, and gives it a span of 1 column
	 * This is equivalent to calling initializeButtonWidget(container, text, griddata, style, 1)
	 *
	 * @param container the Composite that the widget will be placed on
	 * @param text the caption on the button
	 * @param griddata the GridData object that specifies the widget's layout
	 * @param style the style for the button (see Eclipse SWT Button API for
	 * more details)
	 * @param span the span in columns of the widget
	 * @return Button control
	 */
	public static Button initializeButtonWidget(Composite container, String text, GridData griddata, int style) {
		return initializeButtonWidget(container, text, griddata, style, 1);
	}
}
