/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javadocexport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.util.XmlProcessorFactoryJdtUi;

public class JavadocWriter {

	private static final char PATH_SEPARATOR= '/'; // use forward slash for all platforms

	private final IJavaProject[] fJavaProjects;
	private final IPath fBasePath;

	/**
	 * Create a JavadocWriter.
	 * @param basePath The base path to which all path will be made relative (if
	 * possible). If <code>null</code>, paths are not made relative.
	 * @param projects
	 */
	public JavadocWriter(IPath basePath, IJavaProject[] projects) {
		fBasePath= basePath;
		fJavaProjects= projects;
	}

	public Element createXML(JavadocOptionsManager store) throws ParserConfigurationException {
		DocumentBuilderFactory factory= XmlProcessorFactoryJdtUi.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
		factory.setValidating(false);
		DocumentBuilder docBuilder= factory.newDocumentBuilder();
		Document document= docBuilder.newDocument();

		// Create the document
		Element project= document.createElement("project"); //$NON-NLS-1$
		document.appendChild(project);

		project.setAttribute("default", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$

		Element javadocTarget= document.createElement("target"); //$NON-NLS-1$
		project.appendChild(javadocTarget);
		javadocTarget.setAttribute("name", "javadoc"); //$NON-NLS-1$ //$NON-NLS-2$

		Element xmlJavadocDesc= document.createElement("javadoc"); //$NON-NLS-1$
		javadocTarget.appendChild(xmlJavadocDesc);

		if (!store.isFromStandard())
			xmlWriteDoclet(store, document, xmlJavadocDesc);
		else
			xmlWriteJavadocStandardParams(store, document, xmlJavadocDesc);

		return xmlJavadocDesc;
	}

	/**
	 * Writes the document to the given stream.
	 * It is the client's responsibility to close the output stream.
	 * @param javadocElement the XML element defining the Javadoc tags
	 * @param encoding the encoding to use
	 * @param outputStream the output stream
	 * @throws TransformerException thrown if writing fails
	 */
	public static void writeDocument(Element javadocElement, String encoding, OutputStream outputStream) throws TransformerException {

		// Write the document to the stream
		Transformer transformer= XmlProcessorFactoryJdtUi.createTransformerFactoryWithErrorOnDOCTYPE().newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","4"); //$NON-NLS-1$ //$NON-NLS-2$
		DOMSource source = new DOMSource(javadocElement.getOwnerDocument());
		StreamResult result = new StreamResult(new BufferedOutputStream(outputStream));
		transformer.transform(source, result);

	}

	//writes ant file, for now only worry about one project
	private void xmlWriteJavadocStandardParams(JavadocOptionsManager store, Document document, Element xmlJavadocDesc) throws DOMException {

		String destination= getPathString(Path.fromOSString(store.getDestination()));

		xmlJavadocDesc.setAttribute(JavadocOptionsManager.DESTINATION, destination);
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.VISIBILITY, store.getAccess());
		String source= store.getSource();
		if (source.length() > 0 && !"-".equals(source)) { //$NON-NLS-1$
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.SOURCE, store.getSource());
		}
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.USE, booleanToString(store.getBoolean("use"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.NOTREE, booleanToString(store.getBoolean("notree"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.NONAVBAR, booleanToString(store.getBoolean("nonavbar"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.NOINDEX, booleanToString(store.getBoolean("noindex"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.SPLITINDEX, booleanToString(store.getBoolean("splitindex"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.AUTHOR, booleanToString(store.getBoolean("author"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.VERSION, booleanToString(store.getBoolean("version"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.NODEPRECATEDLIST, booleanToString(store.getBoolean("nodeprecatedlist"))); //$NON-NLS-1$
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.NODEPRECATED, booleanToString(store.getBoolean("nodeprecated"))); //$NON-NLS-1$


		//set the packages and source files
		List<String> packages= new ArrayList<>();
		List<String> sourcefiles= new ArrayList<>();
		sortSourceElement(store.getSourceElements(), sourcefiles, packages);
		if (!packages.isEmpty())
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.PACKAGENAMES, toSeparatedList(packages));

		if (!sourcefiles.isEmpty())
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.SOURCEFILES, toSeparatedList(sourcefiles));

		xmlJavadocDesc.setAttribute(JavadocOptionsManager.SOURCEPATH, getPathString(store.getSourcepath()));
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.CLASSPATH, getPathString(store.getClasspath()));

		String overview= store.getOverview();
		if (overview.length() > 0)
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.OVERVIEW, overview);

		String styleSheet= store.getStyleSheet();
		if (styleSheet.length() > 0)
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.STYLESHEETFILE, styleSheet);

		String title= store.getTitle();
		if (title.length() > 0)
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.TITLE, title);


		String vmArgs= store.getVMParams();
		String additionalArgs= store.getAdditionalParams();
		if (vmArgs.length() + additionalArgs.length() > 0) {
			String str= vmArgs + ' ' + additionalArgs;
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.EXTRAOPTIONS, str);
		}

		for (String href : store.getHRefs()) {
			Element links= document.createElement("link"); //$NON-NLS-1$
			xmlJavadocDesc.appendChild(links);
			links.setAttribute(JavadocOptionsManager.HREF, href);
		}
	}

	private void sortSourceElement(IJavaElement[] iJavaElements, List<String> sourcefiles, List<String> packages) {
		for (IJavaElement element : iJavaElements) {
			IPath p= element.getResource().getLocation();
			if (p == null)
				continue;

			if (element instanceof ICompilationUnit) {
				String relative= getPathString(p);
				sourcefiles.add(relative);
			} else if (element instanceof IPackageFragment) {
				packages.add(element.getElementName());
			}
		}
	}

	private String getPathString(IPath[] paths) {
		StringBuilder buf= new StringBuilder();

		for (IPath path : paths) {
			if (buf.length() != 0) {
				buf.append(File.pathSeparatorChar);
			}
			buf.append(getPathString(path));
		}

		if (buf.length() == 0) {
			buf.append('.');
		}
		return buf.toString();
	}

	private boolean hasSameDevice(IPath p1, IPath p2) {
		String dev= p1.getDevice();
		if (dev == null) {
			return p2.getDevice() == null;
		}
		return dev.equals(p2.getDevice());
	}

	//make the path relative to the base path
	private String getPathString(IPath fullPath) {
		if (fBasePath == null || !hasSameDevice(fullPath, fBasePath)) {
			return fullPath.toOSString();
		}
		int matchingSegments= fBasePath.matchingFirstSegments(fullPath);
		if (fBasePath.segmentCount() == matchingSegments) {
			return getRelativePath(fullPath, matchingSegments);
		}
		for (IJavaProject javaProject : fJavaProjects) {
			IProject proj= javaProject.getProject();
			IPath projLoc= proj.getLocation();
			if (projLoc != null && projLoc.segmentCount() <= matchingSegments && projLoc.isPrefixOf(fullPath)) {
				return getRelativePath(fullPath, matchingSegments);
			}
		}
		IPath workspaceLoc= ResourcesPlugin.getWorkspace().getRoot().getLocation();
		if (workspaceLoc.segmentCount() <= matchingSegments && workspaceLoc.isPrefixOf(fullPath)) {
			return getRelativePath(fullPath, matchingSegments);
		}
		return fullPath.toOSString();
	}

	private String getRelativePath(IPath fullPath, int matchingSegments) {
		StringBuilder res= new StringBuilder();
		int backSegments= fBasePath.segmentCount() - matchingSegments;
		while (backSegments > 0) {
			res.append(".."); //$NON-NLS-1$
			res.append(PATH_SEPARATOR);
			backSegments--;
		}
		int segCount= fullPath.segmentCount();
		for (int i= matchingSegments; i < segCount; i++) {
			if (i > matchingSegments) {
				res.append(PATH_SEPARATOR);
			}
			res.append(fullPath.segment(i));
		}
		return res.toString();
	}

	private void xmlWriteDoclet(JavadocOptionsManager store, Document document, Element xmlJavadocDesc) throws DOMException {

		//set the packages and source files
		List<String> packages= new ArrayList<>();
		List<String> sourcefiles= new ArrayList<>();
		sortSourceElement(store.getSourceElements(), sourcefiles, packages);
		if (!packages.isEmpty())
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.PACKAGENAMES, toSeparatedList(packages));

		if (!sourcefiles.isEmpty())
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.SOURCEFILES, toSeparatedList(sourcefiles));

		xmlJavadocDesc.setAttribute(JavadocOptionsManager.SOURCEPATH, getPathString(store.getSourcepath()));
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.CLASSPATH, getPathString(store.getClasspath()));
		xmlJavadocDesc.setAttribute(JavadocOptionsManager.VISIBILITY, store.getAccess());

		Element doclet= document.createElement("doclet"); //$NON-NLS-1$
		xmlJavadocDesc.appendChild(doclet);
		doclet.setAttribute(JavadocOptionsManager.NAME, store.getDocletName());
		doclet.setAttribute(JavadocOptionsManager.PATH, store.getDocletPath());

		String str= store.getOverview();
		if (str.length() > 0)
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.OVERVIEW, str);

		str= store.getAdditionalParams();
		if (str.length() > 0)
			xmlJavadocDesc.setAttribute(JavadocOptionsManager.EXTRAOPTIONS, str);

	}

	private String toSeparatedList(List<String> packages) {
		StringBuilder buf= new StringBuilder();
		Iterator<String> iter= packages.iterator();
		int nAdded= 0;
		while (iter.hasNext()) {
			if (nAdded > 0) {
				buf.append(',');
			}
			nAdded++;
			String curr= iter.next();
			buf.append(curr);
		}
		return buf.toString();
	}

	private String booleanToString(boolean bool) {
		if (bool)
			return "true"; //$NON-NLS-1$
		else
			return "false"; //$NON-NLS-1$
	}

}
