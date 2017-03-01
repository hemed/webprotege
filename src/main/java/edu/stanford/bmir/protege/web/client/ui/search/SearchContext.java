package edu.stanford.bmir.protege.web.client.ui.search;


import com.google.common.base.Optional;
import edu.stanford.bmir.protege.web.shared.selection.SelectionModel;
import edu.stanford.smi.protege.model.ValueType;
import org.semanticweb.owlapi.model.OWLClass;

import java.io.Serializable;


/**
 * I believe we need to add a search context,
 * is such away thatw e restrict search for a particular context,
 * whether be it a class, or any type.
 * @author Hemed
 */

public class SearchContext implements Serializable {
    private static final long serialVersionUID = 5L;
    transient private ValueType type;
    private OWLClass clazz;
    transient SelectionModel selectionModel;

    public Optional getSelectedClass(){
        return selectionModel.getLastSelectedClassData();
    }
}
