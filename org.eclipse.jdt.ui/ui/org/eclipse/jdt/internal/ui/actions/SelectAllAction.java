/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
 
/**
 * This action selects all entries currently showing in view.
 */
public class SelectAllAction extends Action {

	private TableViewer fViewer;

	/**
	 * Creates the action.
	 */
	public SelectAllAction(TableViewer viewer) {
		super("selectAll"); //$NON-NLS-1$
		setText(ActionMessages.getString("SelectAllAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("SelectAllAction.tooltip")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SELECT_ALL_ACTION);
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

	/**
	 * Selects all resources in the view.
	 */
	public void run() {
		fViewer.getTable().selectAll();
		// force viewer selection change
		fViewer.setSelection(fViewer.getSelection());
	}
}
