/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;


public class LocalDeclarationAnalyzer extends ASTVisitor {

	private Selection fSelection;
	private List fAffectedLocals;

	public static VariableDeclaration[] perform(MethodDeclaration method, Selection selection) {
		LocalDeclarationAnalyzer analyzer= new LocalDeclarationAnalyzer(selection);
		method.accept(analyzer);
		return (VariableDeclaration[]) analyzer.fAffectedLocals.toArray(new VariableDeclaration[analyzer.fAffectedLocals.size()]);
	}

	private LocalDeclarationAnalyzer(Selection selection) {
		fSelection= selection;
		fAffectedLocals= new ArrayList(1);
	}
	
	public boolean visit(SimpleName node) {
		IVariableBinding binding= null; 
		if (!considerNode(node) || (binding= ASTNodes.getLocalVariableBinding(node)) == null)
			return false;
		handleReferenceToLocal(node, binding);
		return true;
	}	
	
	private boolean considerNode(ASTNode node) {
		return fSelection.getVisitSelectionMode(node) == Selection.AFTER;
	}
	
	private void handleReferenceToLocal(SimpleName node, IVariableBinding binding) {
		VariableDeclaration declaration= ASTNodes.findVariableDeclaration(binding, node);
		if (declaration != null && fSelection.covers(declaration))
			addLocalDeclaration(declaration);
	}
	
	private void addLocalDeclaration(VariableDeclaration declaration) {
		if (!fAffectedLocals.contains(declaration))
			fAffectedLocals.add(declaration);
	}
}
