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
package org.eclipse.jdt.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.SourceAttachmentBlock;

/**
 * Property page to configure a archive's JARs source attachment
 */
public class SourceAttachmentPropertyPage extends PropertyPage implements IStatusChangeListener {

	private SourceAttachmentBlock fSourceAttachmentBlock;
	private IPackageFragmentRoot fRoot;
	private IPath fContainerPath;

	public SourceAttachmentPropertyPage() {
	}

	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.SOURCE_ATTACHMENT_PROPERTY_PAGE);
	}		
	
	/*
	 * @see PreferencePage#createContents
	 */
	protected Control createContents(Composite composite) {
		initializeDialogUnits(composite);
		Control result= createPageContent(composite);
		Dialog.applyDialogFont(result);
		return result;
	}
	
	private Control createPageContent(Composite composite) {
		try {
			fRoot= getJARPackageFragmentRoot();
			if (fRoot == null || fRoot.getKind() != IPackageFragmentRoot.K_BINARY) {
				return createMessageContent(composite, PreferencesMessages.getString("SourceAttachmentPropertyPage.noarchive.message"));  //$NON-NLS-1$
			}
	
			IPath containerPath= null;
			IJavaProject jproject= fRoot.getJavaProject();
			IClasspathEntry entry= fRoot.getRawClasspathEntry();
			if (entry == null) {
				// use a dummy entry to use for initialization
				entry= JavaCore.newLibraryEntry(fRoot.getPath(), null, null);
			} else {
				if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					containerPath= entry.getPath();
					entry= JavaModelUtil.getClasspathEntryToEdit(jproject, containerPath, fRoot.getPath());
					if (entry == null) {
						IClasspathContainer container= JavaCore.getClasspathContainer(containerPath, jproject);
						String containerName= container != null ? container.getDescription() : containerPath.toString();
						return createMessageContent(composite, PreferencesMessages.getFormattedString("SourceAttachmentPropertyPage.containerentry.message", containerName));  //$NON-NLS-1$
					}
				}
			}
			fContainerPath= containerPath;
			
			fSourceAttachmentBlock= new SourceAttachmentBlock(this, entry);
			return fSourceAttachmentBlock.createControl(composite);				
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return createMessageContent(composite, PreferencesMessages.getString("SourceAttachmentPropertyPage.noarchive.message"));  //$NON-NLS-1$
		}
	}
	
	
	private Control createMessageContent(Composite composite, String message) {
		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;		
		inner.setLayout(layout);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint= convertWidthInCharsToPixels(80);
		
		Label label= new Label(inner, SWT.LEFT + SWT.WRAP);
		label.setText(message);
		label.setLayoutData(gd);
		return inner;
	}
	

	/*
	 * @see IPreferencePage#performOk
	 */
	public boolean performOk() {
		if (fSourceAttachmentBlock != null) {
			try {
				IClasspathEntry entry= fSourceAttachmentBlock.getNewEntry();
				IRunnableWithProgress runnable= SourceAttachmentBlock.getRunnable(getShell(), entry, fRoot.getJavaProject(), fContainerPath);		
				PlatformUI.getWorkbench().getProgressService().run(true, true, runnable);						
			} catch (InvocationTargetException e) {
				String title= PreferencesMessages.getString("SourceAttachmentPropertyPage.error.title"); //$NON-NLS-1$
				String message= PreferencesMessages.getString("SourceAttachmentPropertyPage.error.message"); //$NON-NLS-1$
				ExceptionHandler.handle(e, getShell(), title, message);
				return false;
			} catch (InterruptedException e) {
				// cancelled
				return false;
			}				
		}
		return true;
	}
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		if (fSourceAttachmentBlock != null) {
			fSourceAttachmentBlock.setDefaults();
		}
		super.performDefaults();
	}	
				
	private IPackageFragmentRoot getJARPackageFragmentRoot() throws CoreException {
		// try to find it as Java element (needed for external jars)
		IAdaptable adaptable= getElement();
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem instanceof IPackageFragmentRoot) {
			return (IPackageFragmentRoot) elem;
		}
		// not on classpath or not in a java project
		IResource resource= (IResource) adaptable.getAdapter(IResource.class);
		if (resource instanceof IFile) {
			IProject proj= resource.getProject();
			if (proj.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jproject= JavaCore.create(proj);
				return jproject.getPackageFragmentRoot(resource);
			}
		}
		return null;		
	}
		

	/*
	 * @see IStatusChangeListener#statusChanged
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}	



}
