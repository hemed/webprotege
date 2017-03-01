package edu.stanford.bmir.protege.web.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import edu.stanford.bmir.protege.web.client.rpc.data.*;
import edu.stanford.bmir.protege.web.client.ui.search.SearchContext;

import java.util.List;


/**
 * A service for accessing ontology data.
 * <p />
 *
 * @author Jennifer Vendetti <vendetti@stanford.edu>
 * @author Tania Tudorache <tudorache@stanford.edu>
 */
@RemoteServiceRelativePath("ontology")
public interface OntologyService extends RemoteService {

    /*
     * Project management methods
     */

    public Boolean hasWritePermission(String projectName, String userName);

    public ImportsData getImportedOntologies(String projectName);

    public List<Triple> getEntityTriples(String projectName, List<String> entities, List<String> properties);

    public List<Triple> getEntityTriples(String projectName, List<String> entities, List<String> properties, List<String> reifiedProps);

    public List<EntityPropertyValues> getEntityPropertyValues(String projectName, List<String> entities, List<String> properties, List<String> reifiedProps);

    public EntityData getRootEntity(String projectName);

    public EntityData getEntity(String projectName, String entityName);

    public List<SubclassEntityData> getSubclasses(String projectName, String className);

    public List<EntityData> moveCls(String projectName, String clsName, String oldParentName, String newParentName, boolean checkForCycles,
            String user, String operationDescription);

    public PaginationData<EntityData> getIndividuals(String projectName, String className, int start, int limit, String sort, String dir);

    public List<EntityData> getParents(String projectName, String className, boolean direct);

    /*
     * Properties methods
     */

    public List<EntityData> getSubproperties(String projectName, String propertyName);

    public void addPropertyValue(String projectName, String entityName, PropertyEntityData propertyEntity,
            EntityData value, String user, String operationDescription);

    public void removePropertyValue(String projectName, String entityName, PropertyEntityData propertyEntity,
            EntityData value, String user, String operationDescription);

    public void replacePropertyValue(String projectName, String entityName, PropertyEntityData propertyEntity,
            EntityData oldValue, EntityData newValue, String user, String operationDescription);

    void setPropertyValues(String projectName, String entityName, PropertyEntityData propertyEntity,
            List<EntityData> values, String user, String operationDescription);

    /*
     * Instance methods
     */
    public EntityData createInstanceValue(String projectName, String instName, String typeName, String subjectEntity,
            String propertyEntity, String user, String operationDescription);

    public EntityData createInstanceValueWithPropertyValue(String projectName, String instName, String typeName, String subjectEntity,
    		String propertyEntity, PropertyEntityData instancePropertyEntity, EntityData valueEntityData, String user, String operationDescription);

    /*
     * Search
     */

    public PaginationData<EntityData> search(String projectName, String searchString, ValueType valueType, int start, int limit, String sort, String dir);

    /**
     * public PaginationData<EntityData> search(String projectName, SearchContext context, String searchString, ValueType valueType, int start, int limit, String sort, String dir);
     **/

    public List<EntityData> search(String projectName, String searchString);

    public List<EntityData> search(String projectName, String searchString, ValueType valueType);

    public List<EntityData> getPathToRoot(String projectName, String entityName);

    /*
     * Util methods
     */

    public String getBioPortalSearchContent(String projectName, String entityName, BioPortalSearchData bpSearchData);

    public String getBioPortalSearchContentDetails(String projectName, BioPortalSearchData bpSearchData,
            BioPortalReferenceData bpRefData);

    public EntityData createExternalReference(String projectName, String entityName, BioPortalReferenceData bpRefData,
            String user, String operationDescription);

    public EntityData replaceExternalReference(String projectName, String entityName, BioPortalReferenceData bpRefData,
                                        EntityData oldValueEntityData,
                                        String user, String operationDescription);
}
