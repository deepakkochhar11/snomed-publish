package com.ihtsdo.snomed.web.controller;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.LocaleResolver;

import com.ihtsdo.snomed.dto.refset.ConceptDto;
import com.ihtsdo.snomed.dto.refset.MemberDto;
import com.ihtsdo.snomed.dto.refset.MembersDto;
import com.ihtsdo.snomed.dto.refset.PlanDto;
import com.ihtsdo.snomed.dto.refset.RefsetDto;
import com.ihtsdo.snomed.exception.ConceptIdNotFoundException;
import com.ihtsdo.snomed.exception.InvalidInputException;
import com.ihtsdo.snomed.exception.InvalidSnomedDateFormatException;
import com.ihtsdo.snomed.exception.MemberNotFoundException;
import com.ihtsdo.snomed.exception.NonUniquePublicIdException;
import com.ihtsdo.snomed.exception.OntologyFlavourNotFoundException;
import com.ihtsdo.snomed.exception.OntologyNotFoundException;
import com.ihtsdo.snomed.exception.OntologyVersionNotFoundException;
import com.ihtsdo.snomed.exception.ProgrammingError;
import com.ihtsdo.snomed.exception.RefsetConceptNotFoundException;
import com.ihtsdo.snomed.exception.RefsetNotFoundException;
import com.ihtsdo.snomed.exception.validation.ValidationException;
import com.ihtsdo.snomed.model.Concept;
import com.ihtsdo.snomed.model.refset.Member;
import com.ihtsdo.snomed.model.refset.Refset;
import com.ihtsdo.snomed.model.xml.XmlRefsetConcept;
import com.ihtsdo.snomed.model.xml.XmlRefsetConcepts;
import com.ihtsdo.snomed.model.xml.XmlRefsetShort;
import com.ihtsdo.snomed.service.refset.MemberService;
import com.ihtsdo.snomed.service.refset.RefsetService;
import com.ihtsdo.snomed.service.refset.parser.RefsetParser.Mode;
import com.ihtsdo.snomed.service.refset.parser.RefsetParserFactory;
import com.ihtsdo.snomed.service.refset.parser.RefsetParserFactory.Parser;
import com.ihtsdo.snomed.web.dto.ImportFileDto;
import com.ihtsdo.snomed.web.dto.RefsetErrorBuilder;
import com.ihtsdo.snomed.web.dto.RefsetResponseDto;
import com.ihtsdo.snomed.web.dto.RefsetResponseDto.Status;
import com.ihtsdo.snomed.web.exception.FieldBindingException;
import com.ihtsdo.snomed.web.exception.GlobalBindingException;
import com.ihtsdo.snomed.web.exception.UnrecognisedFileExtensionException;

@Controller
@RequestMapping("/refsets")
public class RefsetController {
    private static final Logger LOG = LoggerFactory.getLogger(RefsetController.class);
    
    public static final String RF2_MIME_TYPE = "application/vnd.ihtsdo.snomed.rf2.terminology.concept+txt";    

    @Inject
    RefsetService refsetService;
    
    @Inject
    MemberService memberService;    
        
    @Inject
    RefsetErrorBuilder refsetErrorBuilder;
    
    @Inject
    LocaleResolver localeResolver;
    
    @Resource
    private MessageSource messageSource;
    
    
    @RequestMapping(value = "", 
            method = RequestMethod.GET, 
            consumes=MediaType.ALL_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<XmlRefsetShort> getAllRefsets(){
        return getRefsetsDto();
    }    
        
    @RequestMapping(value = "{refsetName}", 
            method = RequestMethod.GET, 
            consumes=MediaType.ALL_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody RefsetDto getRefset(@PathVariable String refsetName){
        return getRefsetDto(refsetName);
    }
    
    @RequestMapping(value = "{refsetName}/members",
            params = "type=list",
            method = RequestMethod.POST, 
            produces = {MediaType.APPLICATION_JSON_VALUE }, 
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMembersByList(@Valid @RequestBody Set<MemberDto> members, @PathVariable String refsetName) 
                    throws RefsetNotFoundException, ConceptIdNotFoundException
    {
        LOG.debug("Controller received request to add new members from a list to refset [{}]", refsetName);
        refsetService.addMembers(members, refsetName);
    }
    
    @RequestMapping(value = "{refsetName}/members/{memberId}",
            method = RequestMethod.DELETE, 
            produces = {MediaType.APPLICATION_JSON_VALUE }, 
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public MemberDto deleteMembership(@PathVariable String refsetName, @PathVariable String memberId) 
                    throws RefsetNotFoundException, ConceptIdNotFoundException, MemberNotFoundException, NonUniquePublicIdException
    {
        LOG.debug("Controller received request to delete member {} from refset {}", memberId, refsetName);
        return MemberDto.parse(refsetService.deleteMembership(refsetName, memberId));
    }
    
    @RequestMapping(value = "{refsetName}",
            method = RequestMethod.DELETE, 
            produces = {MediaType.APPLICATION_JSON_VALUE }, 
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public RefsetDto deleteRefset(@PathVariable String refsetName, @PathVariable String memberId) throws RefsetNotFoundException
    {
        LOG.debug("Controller received request to delete refset {}", refsetName);
        return RefsetDto.parse(refsetService.delete(refsetName));
    }     
    
    
    @RequestMapping(value = "{refsetName}/members",
            params = "type=file",
            method = RequestMethod.POST, 
            produces = {MediaType.APPLICATION_JSON_VALUE }, 
            consumes = {MediaType.ALL_VALUE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMembersByFile(@Valid @ModelAttribute ImportFileDto importDto, @PathVariable String refsetName) 
                    throws RefsetNotFoundException, ConceptIdNotFoundException, ProgrammingError, 
                    FieldBindingException, GlobalBindingException
    {
        LOG.debug("Controller received request to add new members to refset [{}] from a file: {}", refsetName, importDto);
        
        try (Reader reader = new InputStreamReader(new BufferedInputStream(importDto.getFile().getInputStream()))){
            Set <MemberDto> membersToImport = RefsetParserFactory.getParser(getParser(importDto)).parseMode(Mode.FORGIVING).parse(reader);
            refsetService.addMembers(membersToImport, refsetName);
            LOG.debug("Imported {} members", membersToImport.size());
        } catch (IOException e) {
            throw new GlobalBindingException(
                    "error.message.unable.to.read.file.io.exception",
                    Arrays.asList(importDto.getFile().getOriginalFilename()),
                    "Unable to read file - general IO exception: " + e.getMessage());
        } catch (InvalidInputException e) {
            throw new ProgrammingError(e);
        } catch (UnrecognisedFileExtensionException e) {
            throw new FieldBindingException(
                    "fileType", 
                    "error.message.file.extension.not.recognised",
                    Arrays.asList(e.getExtension()), 
                    "File extension " + e.getExtension() + "not recognised");
        }
    }   
    
    private Parser getParser(ImportFileDto importFileDto) throws UnrecognisedFileExtensionException{
        LOG.info("Determining parser for file type [" + importFileDto.getFileType() + "]");
        Parser parser = null;
        if (importFileDto.isUseExtension()){
            String name = importFileDto.getFile().getOriginalFilename();
            String extension = name.substring(name.lastIndexOf('.') + 1, name.length()).toUpperCase();
            LOG.info("Determining parser based on file extension [" + extension + "]");
            if (extension.equals("JSON")){
                parser = Parser.JSON;
            }else if (extension.equals("XML")){
                parser = Parser.XML;
            }else if (extension.equals("TXT")){
                parser = Parser.RF2;
            }else if (extension.equals("RF2")){
                parser = Parser.RF2;
            }else{
                throw new UnrecognisedFileExtensionException(extension);
            }
        }else if (importFileDto.isRf2()){
            parser = Parser.RF2;
        }else if (importFileDto.isJson()){
            parser = Parser.JSON;
        }else if (importFileDto.isXml()){
            parser = Parser.XML;
        }else{
            throw new ProgrammingError("You forgot to handle file type enumeration of " + importFileDto.getFileType());
        }
        return parser;
    }    
    
    
    @RequestMapping(value = "{refsetName}/members", 
            method = RequestMethod.GET, 
            produces = {MediaType.APPLICATION_JSON_VALUE }, 
            consumes = {MediaType.ALL_VALUE})
    @ResponseBody   
    @ResponseStatus(HttpStatus.OK)
    public MembersDto getMembers(@PathVariable String refsetName) 
                    throws RefsetNotFoundException, ConceptIdNotFoundException
    {
        LOG.debug("Controller received request to retrieve members for refset [{}]", refsetName);
        List<Member> members = memberService.findByRefsetPublicId(refsetName);
        
        List<MemberDto> memberDtos = new ArrayList<MemberDto>();
        for (Member m : members){
            memberDtos.add(MemberDto.parse(m));
        }
        return new MembersDto(memberDtos);
        
    }
    
        
    
    
    @RequestMapping(value = "{refsetName}/concepts.json", 
            method = RequestMethod.GET, 
            consumes=MediaType.ALL_VALUE,
            produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody XmlRefsetConcepts getConcepts(@PathVariable String refsetName){
        return new XmlRefsetConcepts(getXmlConceptDtos(refsetName));
    }
    
//    @RequestMapping(value = "{refsetName}", 
//            method = RequestMethod.DELETE, 
//            consumes=MediaType.ALL_VALUE,
//            produces=MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<RefsetResponseDto> deleteRefset(HttpServletRequest request, @PathVariable String refsetName){
//        LOG.debug("Received request to delete refset [{}]", refsetName);
//        RefsetResponseDto response = new RefsetResponseDto();
//        response.setPublicId(refsetName);
//        try {
//            Refset deleted = refsetService.delete(refsetName);
//            
//            response.setRefset(
//                RefsetDto.getBuilder(
//                        deleted.getSource(), 
//                        deleted.getType(), 
//                        deleted.getOntologyVersion().getFlavour().getPublicId(),
//                        deleted.getOntologyVersion().getTaggedOn(),
//                        ConceptDto.parse(deleted.getRefsetConcept()),
//                        ConceptDto.parse(deleted.getModuleConcept()),
//                        deleted.getTitle(),
//                        deleted.getDescription(), 
//                        deleted.getPublicId(), 
//                        PlanDto.parse(deleted.getPlan())).build()
//                    );
//
//            response.setCode(RefsetResponseDto.SUCCESS_DELETED);
//            response.setStatus(Status.DELETED);
//            return new ResponseEntity<RefsetResponseDto>(response, HttpStatus.OK);
//        } catch (RefsetNotFoundException e) {
//            response.setCode(RefsetResponseDto.FAIL_REFSET_NOT_FOUND);
//            response.setStatus(Status.FAIL);
//            response.setGlobalErrors(Arrays.asList(messageSource.getMessage(
//                    "global.error.refset.not.found", 
//                    Arrays.asList(refsetName).toArray(), 
//                    LocaleContextHolder.getLocale())));
//            return new ResponseEntity<RefsetResponseDto>(response, HttpStatus.PRECONDITION_FAILED);
//        }
//    }

    @RequestMapping(value = "", method = RequestMethod.POST, 
    produces = {MediaType.APPLICATION_JSON_VALUE }, 
    consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public ResponseEntity<RefsetResponseDto> createRefset(@Valid @RequestBody RefsetDto refsetDto, 
            BindingResult bindingResult, HttpServletRequest request)
    {
        LOG.debug("Controller received request to create new refset [{}]",
                refsetDto.toString());

        int returnCode = RefsetResponseDto.FAIL;
        RefsetResponseDto response = new RefsetResponseDto();
        
        Refset refset = refsetService.findByPublicId(refsetDto.getPublicId());
        if (refset != null){
            bindingResult.addError(new FieldError(
                    bindingResult.getObjectName(), "publicId", refsetDto.getPublicId(),
                    false, null,null, "xml.response.error.publicid.not.unique"));
        }

        if (bindingResult.hasErrors()) {
            return new ResponseEntity<RefsetResponseDto>(refsetErrorBuilder.build(bindingResult, response, returnCode), HttpStatus.NOT_ACCEPTABLE);
        }

        try {
            Refset created = refsetService.create(refsetDto);
            if (created == null){
                return new ResponseEntity<RefsetResponseDto>(refsetErrorBuilder.build(bindingResult, response, RefsetResponseDto.FAIL), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<RefsetResponseDto>(success(response, created, Status.CREATED, RefsetResponseDto.SUCCESS_CREATED), HttpStatus.CREATED);
        } catch (NonUniquePublicIdException e) {
            bindingResult.addError(new FieldError(
                    bindingResult.getObjectName(), "publicId", refsetDto.getPublicId(),
                    false, null, null, "xml.response.error.publicid.not.unique"));
            return new ResponseEntity<RefsetResponseDto>(refsetErrorBuilder.build(bindingResult, response, returnCode), HttpStatus.NOT_ACCEPTABLE);
        } catch (RefsetConceptNotFoundException e) {
            LOG.debug("Create refset failed", e);
            response.setStatus(Status.FAIL);
            response.setCode(RefsetResponseDto.FAIL_VALIDATION);
        } catch (ValidationException e) {
            LOG.debug("Create refset failed", e);
            response.setStatus(Status.FAIL);
            response.setCode(RefsetResponseDto.FAIL_VALIDATION);
        } catch (OntologyNotFoundException e) {
            LOG.debug("Create refset failed", e);
            response.setStatus(Status.FAIL);
            response.setCode(RefsetResponseDto.FAIL_VALIDATION);
        } catch (InvalidSnomedDateFormatException e) {
            bindingResult.addError(new FieldError(
                    bindingResult.getObjectName(), "snomedReleaseDate", refsetDto.getSnomedReleaseDate(),
                    false, new String[] {e.getDateString()},null, "xml.response.error.invalid.date.format"));
            return new ResponseEntity<RefsetResponseDto>(refsetErrorBuilder.build(bindingResult, response, returnCode), HttpStatus.NOT_ACCEPTABLE);
        } catch (OntologyVersionNotFoundException e) {
            String releaseDateString = DateFormat.getDateInstance(DateFormat.LONG, request.getLocale()).format(e.getReleaseDate());            
            bindingResult.addError(new FieldError(
                    bindingResult.getObjectName(), 
                    "snomedExtension", refsetDto.getSnomedReleaseDate(),
                    false, null, null,
                    messageSource.getMessage(
                            "xml.response.error.snomed.version.not.found", 
                            new String[] {e.getFlavour().getLabel(), releaseDateString},
                            Locale.UK)));
            return new ResponseEntity<RefsetResponseDto>(refsetErrorBuilder.build(bindingResult, response, returnCode), HttpStatus.NOT_ACCEPTABLE);
        } catch (OntologyFlavourNotFoundException e) {
            bindingResult.addError(new FieldError(
                    bindingResult.getObjectName(), 
                    "snomedExtension", refsetDto.getSnomedReleaseDate(),
                    false, null, null, 
                    messageSource.getMessage(
                            "xml.response.error.snomed.flavour.not.found", 
                            new String[] {e.getFlavourPublicIdString()},
                            Locale.UK)));
            return new ResponseEntity<RefsetResponseDto>(refsetErrorBuilder.build(bindingResult, response, returnCode), HttpStatus.NOT_ACCEPTABLE);
        }
        
        return new ResponseEntity<RefsetResponseDto>(response, HttpStatus.NOT_ACCEPTABLE);
    }
      
    
    private RefsetResponseDto success(RefsetResponseDto response, Refset updated, Status status, int returnCode) {
        response.setRefset(
                RefsetDto.getBuilder(
                        updated.getSource(), 
                        updated.getType(), 
                        updated.getOntologyVersion().getFlavour().getPublicId(),
                        updated.getOntologyVersion().getTaggedOn(),
                        ConceptDto.parse(updated.getRefsetConcept()),
                        ConceptDto.parse(updated.getModuleConcept()),
                        updated.getTitle(),
                        updated.getDescription(), 
                        updated.getPublicId(), 
                        PlanDto.parse(updated.getPlan())).build()
                    );
        
        response.setStatus(status);
        response.setCode(returnCode);
        return response;
    }

    private List<XmlRefsetConcept> getXmlConceptDtos(String pubId){
        Refset refset = refsetService.findByPublicId(pubId);
        System.out.println("Found refset " + refset);
        Set<Concept> concepts = refset.getPlan().refreshAndGetConcepts();
        List<XmlRefsetConcept> xmlConcepts = new ArrayList<>();
        for (Concept c : concepts){
            xmlConcepts.add(new XmlRefsetConcept(c));
        }
        System.out.println("returning xmlconcepts [" + xmlConcepts.size() + "]");
        return xmlConcepts;
    }    

    private List<XmlRefsetShort> getRefsetsDto(){
        List<Refset> refsets = refsetService.findAll();
        List<XmlRefsetShort> xmlRefsetShorts = new ArrayList<>();
        for (Refset r : refsets){
            xmlRefsetShorts.add(new XmlRefsetShort(r));
        }
        return xmlRefsetShorts;
    }    
 
    private RefsetDto getRefsetDto(String pubId) {
        Refset found = refsetService.findByPublicId(pubId);

        return RefsetDto.getBuilder(
                found.getSource(), 
                found.getType(), 
                found.getOntologyVersion().getFlavour().getPublicId(),
                found.getOntologyVersion().getTaggedOn(),
                ConceptDto.parse(found.getRefsetConcept()),
                ConceptDto.parse(found.getModuleConcept()),
                found.getTitle(),
                found.getDescription(), 
                found.getPublicId(), 
                PlanDto.parse(found.getPlan())).build();
    }
}