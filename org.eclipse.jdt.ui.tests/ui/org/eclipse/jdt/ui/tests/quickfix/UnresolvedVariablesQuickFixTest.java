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
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewCUCompletionUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewVariableCompletionProposal;
import org.eclipse.jdt.internal.ui.text.correction.RenameNodeCompletionProposal;

public class UnresolvedVariablesQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= UnresolvedVariablesQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedVariablesQuickFixTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public static Test suite() {
		return allTests();
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		fJProject1= ProjectTestSetup.getProject();
		
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.NEWTYPE).setPattern("");
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.TYPECOMMENT).setPattern("");
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	public void testVarInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOf("proposals", proposals.size(), 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private Iterator iter;\n");
		buf.append("\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        Iterator iter = vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec, Iterator iter) {\n");
		buf.append("        iter= vec.iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();
	
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });	
	}

	public void testVarInForInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0;;) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int i;\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0;;) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();	
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i = 0;;) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(int i) {\n");
		buf.append("        for (i= 0;;) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });	
	}
	
	public void testVarInForInitializer2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo() {\n");
		buf.append("        for (i= new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] i;\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo() {\n");
		buf.append("        for (i= new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();	
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo() {\n");
		buf.append("        for (int[] i = new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param i \n");
		buf.append("     * @return Returns a number\n");
		buf.append("     */\n");
		buf.append("    int foo(int[] i) {\n");
		buf.append("        for (i= new int[] { 1 };;) {\n");
		buf.append("        }\n");
		buf.append("        return 0;\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });	
	}	
	
	
	public void testVarInInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int i= k;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int k;\n");
		buf.append("    private int i= k;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static final int k = 0;\n");
		buf.append("    private int i= k;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testVarInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var2= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("    public int var2;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var1= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testVarInSuperFieldAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         super.var2= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
	
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         super.var1= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
	
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("    public int var2;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testVarInSuper() throws Exception {
		StringBuffer buf= new StringBuffer();

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test3.E;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         this.color= baz();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);		

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("E.java", buf.toString(), false, null);
		
		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    protected Object olor;\n");
		buf.append("    public test2.E baz() {\n");
		buf.append("        return null;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		pack3.createCompilationUnit("E.java", buf.toString(), false, null);		
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
	
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test3.E;\n");
		buf.append("public class F extends E {\n");
		buf.append("    void foo() {\n");
		buf.append("         this.olor= baz();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
	
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test3.E;\n");
		buf.append("public class F extends E {\n");
		buf.append("    private test2.E color;\n");
		buf.append("\n");
		buf.append("    void foo() {\n");
		buf.append("         this.color= baz();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}	
	
	
	public void testVarInAnonymous() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 6);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            private int fCount;\n");
		buf.append("\n");
		buf.append("            public void run() {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int fCount;\n");
		buf.append("\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int fCount = 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run(int fCount) {\n");
		buf.append("                fCount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected4= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview5= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                fcount= 7;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected5= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(5);
		String preview6= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int fcount) {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected6= buf.toString();			
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5, preview6 }, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });		
	}
	
	public void testLongVarRef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int mash;\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var.hash= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public F var;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int mash;\n");
		buf.append("    private int hash;\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var.hash= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int mash;\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var.mash= 2;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();			
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2}, new String[] { expected1, expected2});		
	}	

	public void testVarAndTypeRef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class F {\n");
		buf.append("    void foo() {\n");
		buf.append("        char ch= Fixe.pathSeparatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 7);
		assertCorrectLabels(proposals);

		boolean doField= true, doParam= true, doLocal= true, doConst= true, doInterface= true, doClass= true, doChange= true;
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCompletionProposal) {
				NewVariableCompletionProposal proposal= (NewVariableCompletionProposal) proposals.get(i);
				String preview= getPreviewContent(proposal);
	
				if (proposal.getVariableKind() == NewVariableCompletionProposal.FIELD) {
					assertTrue("2 field proposals", doField);
					doField= false;
					
					buf= new StringBuffer();
					buf.append("package test1;\n");
					buf.append("import java.io.File;\n");
					buf.append("public class F {\n");
					buf.append("    private Object Fixe;\n");
					buf.append("\n");
					buf.append("    void foo() {\n");
					buf.append("        char ch= Fixe.pathSeparatorChar;\n");
					buf.append("    }\n");
					buf.append("}\n");
					assertEqualString(preview, buf.toString());
				} else if (proposal.getVariableKind() == NewVariableCompletionProposal.LOCAL) {
					assertTrue("2 local proposals", doLocal);
					doLocal= false;
					
					buf= new StringBuffer();
					buf.append("package test1;\n");
					buf.append("import java.io.File;\n");
					buf.append("public class F {\n");
					buf.append("    void foo() {\n");
					buf.append("        Object Fixe;\n");					
					buf.append("        char ch= Fixe.pathSeparatorChar;\n");
					buf.append("    }\n");
					buf.append("}\n");
					assertEqualString(preview, buf.toString());
				} else if (proposal.getVariableKind() == NewVariableCompletionProposal.PARAM) {
					assertTrue("2 param proposals", doParam);
					doParam= false;
					
					buf= new StringBuffer();
					buf.append("package test1;\n");
					buf.append("import java.io.File;\n");
					buf.append("public class F {\n");
					buf.append("    void foo(Object Fixe) {\n");
					buf.append("        char ch= Fixe.pathSeparatorChar;\n");
					buf.append("    }\n");
					buf.append("}\n");
					assertEqualString(preview, buf.toString());
				} else if (proposal.getVariableKind() == NewVariableCompletionProposal.CONST_FIELD) {
					assertTrue("2 const proposals", doConst);
					doConst= false;
					
					buf= new StringBuffer();
					buf.append("package test1;\n");
					buf.append("import java.io.File;\n");
					buf.append("public class F {\n");
					buf.append("    private static final String Fixe = null;\n");
					buf.append("\n");
					buf.append("    void foo() {\n");
					buf.append("        char ch= Fixe.pathSeparatorChar;\n");
					buf.append("    }\n");
					buf.append("}\n");
					assertEqualString(preview, buf.toString());
					
				} else {
					assertTrue("unknown type", false);
				}
			} else if (curr instanceof NewCUCompletionUsingWizardProposal) {
				NewCUCompletionUsingWizardProposal proposal= (NewCUCompletionUsingWizardProposal) curr;
				proposal.setShowDialog(false);
				proposal.apply(null);
				
				ICompilationUnit newCU= pack1.getCompilationUnit("Fixe.java");
				assertTrue("Nothing created", newCU.exists());

				if (proposal.isClass()) {
					assertTrue("2 class proposals", doClass);
					doClass= false;

					buf= new StringBuffer();
					buf.append("package test1;\n");
					buf.append("\n");
					buf.append("public class Fixe {\n");
					buf.append("\n");
					buf.append("}\n");
					assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
					JavaProjectHelper.performDummySearch();
					newCU.delete(true, null);
				} else {
					assertTrue("2 interface proposals", doInterface);
					doInterface= false;					
					
					buf= new StringBuffer();
					buf.append("package test1;\n");
					buf.append("\n");
					buf.append("public interface Fixe {\n");
					buf.append("\n");
					buf.append("}\n");
					assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
					JavaProjectHelper.performDummySearch();
					newCU.delete(true, null);
				}
			} else {
				assertTrue("2 replace proposals", doChange);
				doChange= false;
				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
	
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.io.File;\n");
				buf.append("public class F {\n");
				buf.append("    void foo() {\n");
				buf.append("        char ch= File.pathSeparatorChar;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}		
	
	}
	
	public void testVarWithGenricType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var2= new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", buf.toString(), false, null);		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    protected int var1;\n");
		buf.append("    public ArrayList<String> var2;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class F {\n");
		buf.append("    void foo(E e) {\n");
		buf.append("         e.var1= new ArrayList<String>();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	

	}


	public void testSimilarVariableNames1() throws Exception {
		StringBuffer buf= new StringBuffer();
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public int foo() {\n");
		buf.append("        return count;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCompletionProposal)) {
				proposals.remove(i);
			}
		}
		
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public int foo() {\n");
		buf.append("        return CON1;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public int foo() {\n");
		buf.append("        return cout;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testSimilarVariableNames2() throws Exception {
		StringBuffer buf= new StringBuffer();
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private static short var1= 1;\n");
		buf.append("    private static float var2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        count= x;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCompletionProposal)) {
				proposals.remove(i);
			}
		}
		
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private static short var1= 1;\n");
		buf.append("    private static float var2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        var2= x;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private static final short CON1= 1;\n");
		buf.append("    private static final float CON2= 1.0f;\n");
		buf.append("    private static short var1= 1;\n");
		buf.append("    private static float var2= 1.0f;\n");
		buf.append("    private String bla;\n");
		buf.append("    private String cout;\n");
		buf.append("    public void foo(int x) {\n");
		buf.append("        cout= x;\n");		
		buf.append("    }\n");				
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testSimilarVariableNamesMultipleOcc() throws Exception {
		StringBuffer buf= new StringBuffer();
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private int cout;\n");
		buf.append("    public void setCount(int x) {\n");
		buf.append("        count= x;\n");
		buf.append("        count++;\n");	
		buf.append("    }\n");
		buf.append("    public int getCount() {\n");
		buf.append("        return count;\n");	
		buf.append("    }\n");			
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCompletionProposal)) {
				proposals.remove(i);
			}
		}
		
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class E {\n");
		buf.append("    private int cout;\n");
		buf.append("    public void setCount(int x) {\n");
		buf.append("        cout= x;\n");
		buf.append("        cout++;\n");	
		buf.append("    }\n");
		buf.append("    public int getCount() {\n");
		buf.append("        return cout;\n");	
		buf.append("    }\n");			
		buf.append("}\n");
		String expected1= buf.toString();

		assertEqualString( preview1, expected1);
	}
	
	public void testVarMultipleOccurances1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0; i > 9; i++) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal localProposal= null;
		for (int i = 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCompletionProposal && ((NewVariableCompletionProposal) curr).getVariableKind() == NewVariableCompletionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);
		
		String preview= getPreviewContent(localProposal);
		buf= new StringBuffer();	
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i = 0; i > 9; i++) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualString(preview, expected);	
	}	
	
	public void testVarMultipleOccurances2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i= 0; i > 9;) {\n");
		buf.append("            i++;\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal localProposal= null;
		for (int i = 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCompletionProposal && ((NewVariableCompletionProposal) curr).getVariableKind() == NewVariableCompletionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);
		
		String preview= getPreviewContent(localProposal);
		buf= new StringBuffer();	
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (int i = 0; i > 9;) {\n");
		buf.append("            i++;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualString(preview, expected);	
	}
	
	public void testVarMultipleOccurances3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        for (i = 0; i > 9;) {\n");
		buf.append("        }\n");
		buf.append("        i= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal localProposal= null;
		for (int i = 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (curr instanceof NewVariableCompletionProposal && ((NewVariableCompletionProposal) curr).getVariableKind() == NewVariableCompletionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);
		
		String preview= getPreviewContent(localProposal);
		buf= new StringBuffer();	
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        int i;\n");
		buf.append("        for (i = 0; i > 9;) {\n");
		buf.append("        }\n");
		buf.append("        i= 9;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		assertEqualString(preview, expected);	
	}	

	public void testVarInArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] arr) {\n");
		buf.append("        for (int i = 0; i > arr.lenght; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] arr) {\n");
		buf.append("        for (int i = 0; i > arr.length; i++) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });	
	}

}
