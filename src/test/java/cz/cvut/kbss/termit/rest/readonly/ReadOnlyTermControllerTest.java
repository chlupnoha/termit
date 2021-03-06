package cz.cvut.kbss.termit.rest.readonly;

import com.fasterxml.jackson.core.type.TypeReference;
import cz.cvut.kbss.termit.dto.readonly.ReadOnlyTerm;
import cz.cvut.kbss.termit.environment.Environment;
import cz.cvut.kbss.termit.environment.Generator;
import cz.cvut.kbss.termit.exception.NotFoundException;
import cz.cvut.kbss.termit.model.Vocabulary;
import cz.cvut.kbss.termit.rest.BaseControllerTestRunner;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.business.readonly.ReadOnlyTermService;
import cz.cvut.kbss.termit.util.ConfigParam;
import cz.cvut.kbss.termit.util.Configuration;
import cz.cvut.kbss.termit.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cvut.kbss.termit.util.Constants.DEFAULT_PAGE_SPEC;
import static cz.cvut.kbss.termit.util.Constants.DEFAULT_TERM_NAMESPACE_SEPARATOR;
import static cz.cvut.kbss.termit.util.Constants.QueryParams.PAGE;
import static cz.cvut.kbss.termit.util.Constants.QueryParams.PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReadOnlyTermControllerTest extends BaseControllerTestRunner {

    private static final String PATH = "/public/vocabularies/";
    private static final String VOCABULARY_NAME = "metropolitan-plan";
    private static final String TERM_NAME = "locality";
    private static final String VOCABULARY_URI = Environment.BASE_URI + "/" + VOCABULARY_NAME;
    private static final String NAMESPACE = VOCABULARY_URI + Constants.DEFAULT_TERM_NAMESPACE_SEPARATOR + "/";

    @Mock
    private ReadOnlyTermService termService;

    @Mock
    private IdentifierResolver idResolver;

    @Mock
    private Configuration configuration;

    private Vocabulary vocabulary;

    @InjectMocks
    private ReadOnlyTermController sut;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.vocabulary = Generator.generateVocabulary();
        vocabulary.setUri(URI.create(VOCABULARY_URI));
        when(configuration.get(ConfigParam.NAMESPACE_VOCABULARY)).thenReturn(Environment.BASE_URI);
        when(configuration.get(ConfigParam.TERM_NAMESPACE_SEPARATOR)).thenReturn(DEFAULT_TERM_NAMESPACE_SEPARATOR);
        when(idResolver.resolveIdentifier(Environment.BASE_URI, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        when(idResolver.resolveIdentifier(ConfigParam.NAMESPACE_VOCABULARY, VOCABULARY_NAME))
                .thenReturn(URI.create(VOCABULARY_URI));
        setUp(sut);
    }

    @Test
    void getAllReturnsAllTermsFromVocabularyFromService() throws Exception {
        when(idResolver.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final List<ReadOnlyTerm> terms = generateTerms();
        when(termService.findVocabularyRequired(URI.create(VOCABULARY_URI))).thenReturn(vocabulary);
        when(termService.findAll(any())).thenReturn(terms);

        final MvcResult mvcResult = mockMvc.perform(get(PATH + VOCABULARY_NAME + "/terms")).andExpect(status().isOk())
                                           .andReturn();
        final List<ReadOnlyTerm> result = readValue(mvcResult, new TypeReference<List<ReadOnlyTerm>>() {
        });
        assertEquals(terms, result);
        verify(termService).findAll(vocabulary);
    }

    private List<ReadOnlyTerm> generateTerms() {
        return Generator.generateTermsWithIds(5).stream().map(ReadOnlyTerm::new).collect(Collectors.toList());
    }

    @Test
    void getAllWithSearchStringUsesServiceToRetrieveMatchingTermsAndReturnsThem() throws Exception {
        when(idResolver.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final List<ReadOnlyTerm> terms = generateTerms();
        when(termService.findVocabularyRequired(URI.create(VOCABULARY_URI))).thenReturn(vocabulary);
        when(termService.findAll(any(), any())).thenReturn(terms);
        final String searchString = "test";

        final MvcResult mvcResult = mockMvc.perform((get(PATH + VOCABULARY_NAME + "/terms"))
                .param(Constants.QueryParams.NAMESPACE, Environment.BASE_URI).param("searchString", searchString))
                                           .andExpect(status().isOk())
                                           .andReturn();
        final List<ReadOnlyTerm> result = readValue(mvcResult, new TypeReference<List<ReadOnlyTerm>>() {
        });
        assertEquals(terms, result);
        verify(termService).findAll(searchString, vocabulary);
    }

    @Test
    void getAllWithSearchStringAndIncludeImportedUsesServiceToRetrieveMatchingTermsIncludingImportedOnesAndReturnsThem()
            throws Exception {
        when(idResolver.buildNamespace(eq(VOCABULARY_URI), any())).thenReturn(NAMESPACE);
        final List<ReadOnlyTerm> terms = generateTerms();
        when(termService.findVocabularyRequired(URI.create(VOCABULARY_URI))).thenReturn(vocabulary);
        when(termService.findAllIncludingImported(any(), any())).thenReturn(terms);
        final String searchString = "test";

        final MvcResult mvcResult = mockMvc.perform((get(PATH + VOCABULARY_NAME + "/terms"))
                .param(Constants.QueryParams.NAMESPACE, Environment.BASE_URI).param("searchString", searchString)
                .param("includeImported", Boolean.TRUE.toString()))
                                           .andExpect(status().isOk())
                                           .andReturn();
        final List<ReadOnlyTerm> result = readValue(mvcResult, new TypeReference<List<ReadOnlyTerm>>() {
        });
        assertEquals(terms, result);
        verify(termService).findAllIncludingImported(searchString, vocabulary);
    }

    @Test
    void getAllRootsLoadsRootsFromCorrectPage() throws Exception {
        final List<ReadOnlyTerm> terms = generateTerms();
        when(termService.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        when(termService.findAllRoots(eq(vocabulary), any(Pageable.class))).thenReturn(terms);
        mockMvc.perform(get(PATH + VOCABULARY_NAME + "/terms/roots").param(PAGE, "5").param(PAGE_SIZE, "100"))
               .andExpect(status().isOk());

        final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(termService).findAllRoots(eq(vocabulary), captor.capture());
        assertEquals(PageRequest.of(5, 100), captor.getValue());
    }

    @Test
    void getAllRootsCreatesDefaultPageRequestWhenPagingInfoIsNotSpecified() throws Exception {
        final List<ReadOnlyTerm> terms = generateTerms();
        when(termService.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        when(termService.findAllRoots(eq(vocabulary), any(Pageable.class))).thenReturn(terms);
        mockMvc.perform(get(PATH + VOCABULARY_NAME + "/terms/roots")).andExpect(status().isOk());

        final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(termService).findAllRoots(eq(vocabulary), captor.capture());
        assertEquals(DEFAULT_PAGE_SPEC, captor.getValue());
    }

    @Test
    void getAllRootsRetrievesRootTermsIncludingImportedWhenParameterIsSpecified() throws Exception {
        final List<ReadOnlyTerm> terms = generateTerms();
        when(termService.findVocabularyRequired(vocabulary.getUri())).thenReturn(vocabulary);
        when(termService.findAllRootsIncludingImported(eq(vocabulary), any(Pageable.class))).thenReturn(terms);
        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + VOCABULARY_NAME + "/terms/roots").param("includeImported", Boolean.TRUE.toString()))
                .andExpect(status().isOk()).andReturn();
        final List<ReadOnlyTerm> result = readValue(mvcResult, new TypeReference<List<ReadOnlyTerm>>() {
        });
        assertEquals(terms, result);
        verify(termService).findAllRootsIncludingImported(eq(vocabulary), any(PageRequest.class));
    }

    @Test
    void getByIdRetrievesTermFromService() throws Exception {
        final ReadOnlyTerm term = new ReadOnlyTerm(Generator.generateTerm());
        term.setUri(URI.create(NAMESPACE + TERM_NAME));
        when(idResolver.buildNamespace(VOCABULARY_URI, DEFAULT_TERM_NAMESPACE_SEPARATOR)).thenReturn(NAMESPACE);
        when(idResolver.resolveIdentifier(NAMESPACE, TERM_NAME)).thenReturn(term.getUri());
        when(termService.findRequired(any())).thenReturn(term);
        final MvcResult mvcResult = mockMvc.perform(get(PATH + VOCABULARY_NAME + "/terms/" + TERM_NAME).param(
                Constants.QueryParams.NAMESPACE, Environment.BASE_URI)).andExpect(status().isOk()).andReturn();
        final ReadOnlyTerm result = readValue(mvcResult, ReadOnlyTerm.class);
        assertEquals(term, result);
        verify(termService).findRequired(term.getUri());
    }

    @Test
    void getByIdReturnsNotFoundWhenNotFoundExceptionIsThrownByService() throws Exception {
        when(termService.findRequired(any())).thenThrow(NotFoundException.class);
        mockMvc.perform(get(PATH + VOCABULARY_NAME + "/terms/" + TERM_NAME).param(
                Constants.QueryParams.NAMESPACE, Environment.BASE_URI)).andExpect(status().isNotFound());
    }

    @Test
    void getSubTermsRetrievesSubTermsOfTermFromService() throws Exception {
        final ReadOnlyTerm term = new ReadOnlyTerm(Generator.generateTerm());
        term.setUri(URI.create(NAMESPACE + TERM_NAME));
        when(idResolver.buildNamespace(VOCABULARY_URI, DEFAULT_TERM_NAMESPACE_SEPARATOR)).thenReturn(NAMESPACE);
        when(idResolver.resolveIdentifier(NAMESPACE, TERM_NAME)).thenReturn(term.getUri());
        when(termService.findRequired(any())).thenReturn(term);
        final List<ReadOnlyTerm> subTerms = generateTerms();
        when(termService.findSubTerms(term)).thenReturn(subTerms);

        final MvcResult mvcResult = mockMvc
                .perform(get(PATH + VOCABULARY_NAME + "/terms/" + TERM_NAME + "/subterms").param(
                        Constants.QueryParams.NAMESPACE, Environment.BASE_URI)).andExpect(status().isOk()).andReturn();
        final List<ReadOnlyTerm> result = readValue(mvcResult, new TypeReference<List<ReadOnlyTerm>>() {
        });
        assertEquals(subTerms, result);
        verify(termService).findSubTerms(term);
    }
}
