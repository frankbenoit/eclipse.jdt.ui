package org.eclipse.jdt.ui.tests.quickfix;


import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.ui.JavaUI;

public class MarkerResolutionGenerator implements IMarkerResolutionGenerator {

	/**
	 * Marker resolution that replaces the covered range with the uppercased content.	 */
	public static class TestCorrectionMarkerResolution implements IMarkerResolution {

		public TestCorrectionMarkerResolution() {
		}
	
		/* (non-Javadoc)
		 * @see IMarkerResolution#getLabel()
		 */
		public String getLabel() {
			return "Change to Uppercase";
		}
	
		/* (non-Javadoc)
		 * @see IMarkerResolution#run(IMarker)
		 */
		public void run(IMarker marker) {
			FileEditorInput input= new FileEditorInput((IFile) marker.getResource());
			IAnnotationModel model= JavaUI.getDocumentProvider().getAnnotationModel(input);
			if (model != null) {
				// resource is open in editor
				
				Position pos= findProblemPosition(model, marker);
				if (pos != null) {
					IDocument doc= JavaUI.getDocumentProvider().getDocument(input);
					try {
						String str= doc.get(pos.getOffset(), pos.getLength());
						doc.replace(pos.getOffset(), pos.getLength(), str.toUpperCase());
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			} else {
				// resource is not open in editor
				// to do: work on the resource
			}
			try {
				marker.delete();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		
		private Position findProblemPosition(IAnnotationModel model, IMarker marker) {
			Iterator iter= model.getAnnotationIterator();
			while (iter.hasNext()) {
				Object curr= iter.next();
				if (curr instanceof MarkerAnnotation) {
					MarkerAnnotation annot= (MarkerAnnotation) curr;
					if (marker.equals(annot.getMarker())) {
						return model.getPosition(annot);
					}
				}
			}
			return null;
		}
	}


	public MarkerResolutionGenerator() {
	}

	/* (non-Javadoc)
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] { new TestCorrectionMarkerResolution() };
	}
}
