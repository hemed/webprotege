package edu.stanford.bmir.protege.web.server.notes;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import edu.stanford.bmir.protege.web.server.events.HasPostEvents;
import edu.stanford.bmir.protege.web.server.inject.project.NotesOntologyDocument;
import edu.stanford.bmir.protege.web.server.logging.WebProtegeLogger;
import edu.stanford.bmir.protege.web.server.owlapi.WebProtegeOWLManager;
import edu.stanford.bmir.protege.web.shared.BrowserTextProvider;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import edu.stanford.bmir.protege.web.shared.entity.OWLEntityData;
import edu.stanford.bmir.protege.web.shared.event.ProjectEvent;
import edu.stanford.bmir.protege.web.shared.notes.*;
import edu.stanford.bmir.protege.web.shared.notes.NoteType;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.protege.notesapi.NotesException;
import org.protege.notesapi.notes.*;
import org.protege.notesapi.notes.impl.DefaultComment;
import org.protege.notesapi.oc.impl.DefaultOntologyComponent;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentSerializer;
import org.semanticweb.binaryowl.change.OntologyChangeDataList;
import org.semanticweb.binaryowl.owlapi.BinaryOWLOntologyDocumentFormat;
import org.semanticweb.owlapi.change.OWLOntologyChangeData;
import org.semanticweb.owlapi.change.OWLOntologyChangeRecord;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 20/04/2012
 */
@Deprecated
public class OWLAPINotesManagerNotesAPIImpl implements OWLAPINotesManager {


    public static final String CHANGES_ONTOLOGY_FILE_NAME = "changes.owl";

    public static final IRI CHANGES_ONTOLOGY_IRI = IRI.create("http://protege.stanford.edu/ontologies/ChAO/changes.owl");

    private final WebProtegeLogger logger;

    private final ProjectId projectId;

    private final OWLDataFactory dataFactory;


    private final BrowserTextProvider browserTextProvider;

    private OWLOntology notesOntology;

//    private final NotesManager notesManager;

    private File notesOntologyDocument;

    @Inject
    public OWLAPINotesManagerNotesAPIImpl(@NotesOntologyDocument File notesOntologyDocument,
                                          ProjectId projectId,
                                          OWLDataFactory dataFactory,
                                          HasPostEvents<ProjectEvent<?>> eventManager,
                                          BrowserTextProvider browserTextProvider,
                                          WebProtegeLogger logger) {
        this.logger = logger;
        this.projectId = projectId;
        this.dataFactory = dataFactory;
        this.browserTextProvider = browserTextProvider;
        this.notesOntologyDocument = notesOntologyDocument;

        try {
            Stopwatch stopwatch = Stopwatch.createStarted();
            if(!notesOntologyDocument.exists()) {
                createEmptyNotesOntology();
            }
            else {
                loadExistingNotesOntology();
            }
//            notesManager = NotesManager.createNotesManager(notesOntology, getChangeOntologyDocumentIRI().toString());
//            notesManager.getOWLOntology().getOWLOntologyManager().addOntologyChangeListener(new OWLOntologyChangeListener() {
//                public void ontologiesChanged(List<? extends OWLOntologyChange> changes) throws OWLException {
//                    handleNotesOntologyChanged(Collections.unmodifiableList(changes));
//                }
//            });
            logger.info(projectId, "Initialized notes manager in %d ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
        catch (OWLOntologyCreationException e) {
            // Can't start - too dangerous to do anything without human intervention
            throw new RuntimeException(e);
        }
    }

    private void loadExistingNotesOntology() throws OWLOntologyCreationException {
        final OWLOntologyManager man = WebProtegeOWLManager.createOWLOntologyManager();
        man.addIRIMapper(new SimpleIRIMapper(CHANGES_ONTOLOGY_IRI,  getChangeOntologyDocumentIRI()));
        notesOntology = man.loadOntologyFromOntologyDocument(notesOntologyDocument);
    }


    private void createEmptyNotesOntology() {
        try {
            OWLOntologyManager notesOntologyManager = WebProtegeOWLManager.createOWLOntologyManager();
            notesOntology = notesOntologyManager.createOntology();
            final OWLDataFactory df = notesOntologyManager.getOWLDataFactory();
//            notesOntologyManager.applyChange(new AddImport(notesOntology, df.getOWLImportsDeclaration(CHANGES_ONTOLOGY_IRI)));
            IRI notesOntologyDocumentIRI = IRI.create(notesOntologyDocument);
            notesOntologyManager.setOntologyDocumentIRI(notesOntology, notesOntologyDocumentIRI);
            notesOntologyDocument.getParentFile().mkdirs();
            BinaryOWLOntologyDocumentFormat notesOntologyDocumentFormat = new BinaryOWLOntologyDocumentFormat();
            notesOntologyManager.saveOntology(notesOntology, notesOntologyDocumentFormat, notesOntologyDocumentIRI);
        }
        catch (OWLOntologyCreationException | OWLOntologyStorageException e) {
            throw new RuntimeException(e);
        }
    }


    private void handleNotesOntologyChanged(List<OWLOntologyChange> changes) {
        try {
            OWLOntologyManager notesOntologyManager = notesOntology.getOWLOntologyManager();
            if(notesOntologyManager.getOntologyFormat(notesOntology) instanceof BinaryOWLOntologyDocumentFormat) {
                List<OWLOntologyChangeData> infoList = new ArrayList<OWLOntologyChangeData>();
                for(OWLOntologyChange change : changes) {
                    OWLOntologyChangeRecord rec = change.getChangeRecord();
                    OWLOntologyChangeData info = rec.getData();
                    infoList.add(info);
                }
                BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
                serializer.appendOntologyChanges(notesOntologyDocument, new OntologyChangeDataList(infoList, System.currentTimeMillis()));
            }
            else {
                // Swap it over
                notesOntologyManager.saveOntology(notesOntology, new BinaryOWLOntologyDocumentFormat());
            }

        }
        catch (OWLOntologyStorageException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private IRI getChangeOntologyDocumentIRI() {
        URL changeOntologyURL = OWLAPINotesManagerNotesAPIImpl.class.getResource("/" + CHANGES_ONTOLOGY_FILE_NAME);
        if (changeOntologyURL == null) {
            throw new RuntimeException("Changes ontology not found.  Please make sure the changes ontology document is placed in the class path with a file name of " + CHANGES_ONTOLOGY_FILE_NAME);
        }
        String uriString = changeOntologyURL.toString();
        return IRI.create(uriString);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Note getNoteForAnnotation(Annotation annotation, Optional<NoteId> inReplyTo) {
        UserId author = UserId.getUserId(annotation.getAuthor());
        String body = annotation.getBody() == null ? "" : annotation.getBody();
        long timestamp = annotation.getCreatedAt();
        Optional<String> subject = annotation.getSubject() == null ? Optional.absent() : Optional.of(annotation.getSubject());

        NoteId noteId = NoteId.createNoteIdFromLexicalForm(annotation.getId());
        NoteHeader noteHeader = new NoteHeader(noteId, inReplyTo, author, timestamp);
        NoteStatus noteStatus = annotation.getArchived() != null && annotation.getArchived() ? NoteStatus.RESOLVED : NoteStatus.OPEN;
        NoteContent noteContent = NoteContent.builder().setBody(body).setNoteStatus(noteStatus).setNoteType(NoteType.COMMENT).setSubject(subject).build();
        return Note.createNote(noteHeader, noteContent);
    }

    /**
     * Converts an OWLEntity to an AnnotatableThing.  The entity MUST be either an OWLClass, OWLObjectProperty,
     * OWLDataProperty, OWLAnnotationProperty or OWLNamedIndividual.  This method does not support the conversion
     * of OWLDatatype objects.
     * @param entity The entity to be converted.
     * @return The AnnotatableThing corresponding to the entity.
     */
    private AnnotatableThing getAnnotatableThing(OWLEntity entity) {
        return new DefaultOntologyComponent(dataFactory.getOWLNamedIndividual(entity.getIRI()), notesOntology);
    }
    
    private AnnotatableThing getAnnotatableThingForObjectId(NoteId noteId) {
        OWLNamedIndividual entity = dataFactory.getOWLNamedIndividual(IRI.create(noteId.getLexicalForm()));
        return new DefaultComment(entity, notesOntology);
    }


    @Override
    public void deleteNoteAndReplies(OWLEntity targetEntity, NoteId noteId) {
//        Annotation note = notesManager.getNote(noteId.getLexicalForm());
//        if (note != null) {
//            notesManager.deleteNote(noteId.getLexicalForm());
//            eventManager.postEvent(new NoteDeletedEvent(projectId, noteId));
//            eventManager.postEvent(new EntityNotesChangedEvent(projectId, targetEntity, getIndirectNotesCount(targetEntity)));
//        }
    }


    @Override
    public void setNoteStatus(NoteId noteId, NoteStatus noteStatus) {
//        Annotation note = notesManager.getNote(noteId.getLexicalForm());
//        if(note == null) {
//            // Sometimes we fail to find the note.  I'm not sure why.  This has something to do with the weird internals
//            // and typing of the notes API.
//            logger.info(projectId, "Failed to find note by Id when changing the note status.  The noteId was %s", noteId);
//            return;
//        }
//        if(noteStatus == NoteStatus.OPEN) {
//            note.setArchived(false);
//        }
//        else {
//            note.setArchived(true);
//        }
//        eventManager.postEvent(new NoteStatusChangedEvent(projectId, noteId, noteStatus));
    }



    public int getDirectNotesCount(OWLEntity entity) {
        return getDiscusssionThread(entity).getRootNotes().size();
    }

    public int getIndirectNotesCount(OWLEntity entity) {
        return getDiscusssionThread(entity).size();
    }

    @Override
    public Note addReplyToNote(OWLEntity targetEntity, NoteId inReplyToId, NoteContent replyContent, UserId author) {
        return addReplyToNote(targetEntity, inReplyToId, replyContent, author, System.currentTimeMillis());
    }

    @Override
    public Note addReplyToNote(OWLEntity targetEntity, NoteId inReplyToId, NoteContent replyContent, UserId author, long timestamp) {
        try {
            AnnotatableThing target = getAnnotatableThingForObjectId(inReplyToId);
            Note note = addNoteToTarget(target, replyContent, author, timestamp);
            OWLEntityData entityData = DataFactory.getOWLEntityData(targetEntity,
                                                                    browserTextProvider.getOWLEntityBrowserText(
                                                                            targetEntity).orElse("Entity" ));
            return note;
        }
        catch (NotesException e) {
            throw new RuntimeException("Problem creating note: " + e.getMessage());
        }
    }

    @Override
    public Note addNoteToEntity(OWLEntity targetEntity, NoteContent noteContent, UserId author) {
        return addNoteToEntity(targetEntity, noteContent, author, System.currentTimeMillis());
    }

    @Override
    public Note addNoteToEntity(OWLEntity targetEntity, NoteContent noteContent, UserId author, long timestamp) {
        try {
            checkNotNull(targetEntity);
            checkNotNull(noteContent);
            checkNotNull(author);
            AnnotatableThing target = getAnnotatableThing(targetEntity);
            Note note = addNoteToTarget(target, noteContent, author, timestamp);
            OWLEntityData entityData = DataFactory.getOWLEntityData(targetEntity,
                                                                    browserTextProvider.getOWLEntityBrowserText(
                                                                            targetEntity).orElse("" ));
            return note;
        }
        catch (NotesException e) {
            throw new RuntimeException("Problem creating note: " + e.getMessage());
        }
    }

    private Note addNoteToTarget(AnnotatableThing target, NoteContent noteContent, UserId author, long timestamp) throws NotesException {
        final String subject = noteContent.getSubject().or("");
        final String body = noteContent.getBody().or("");
        final org.protege.notesapi.notes.NoteType noteType = org.protege.notesapi.notes.NoteType.Comment;
//        Annotation annotation = notesManager.createSimpleNote(noteType, subject, body, author.getUserName(), target);
//        annotation.setCreatedAt(timestamp);
//        final NoteId noteId = NoteId.createNoteIdFromLexicalForm(annotation.getId());
        NoteId noteId = NoteId.createNoteIdFromLexicalForm("ABC");
        NoteHeader noteHeader = new NoteHeader(noteId, Optional.absent(), author, timestamp);
        return Note.createNote(noteHeader, noteContent);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public DiscussionThread getDiscusssionThread(OWLEntity targetEntity) {
//        AnnotatableThing annotatableThing = getAnnotatableThing(targetEntity);
        Set<Note> result = new HashSet<Note>();
//        for(Annotation annotation : annotatableThing.getAssociatedAnnotations()) {
//            if (annotation != null) {
//                getAllNotesForAnnotation(annotation, Optional.<NoteId>absent(), result);
//            }
//        }
        return new DiscussionThread(result);
    }


    private void getAllNotesForAnnotation(Annotation annotation, Optional<NoteId> inReplyTo, Set<Note> result) {
        final Note noteForAnnotation = getNoteForAnnotation(annotation, inReplyTo);
        result.add(noteForAnnotation);
        for(Annotation anno : annotation.getAssociatedAnnotations()) {
            getAllNotesForAnnotation(anno, Optional.of(noteForAnnotation.getNoteId()), result);
        }
    }

}
