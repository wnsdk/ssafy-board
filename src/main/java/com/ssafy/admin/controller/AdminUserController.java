package com.ssafy.admin.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.member.model.MemberDto;
import com.ssafy.member.model.service.MemberService;

@RestController
@RequestMapping("/admin")
@CrossOrigin("*")
public class AdminUserController {

	private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);
	
	private MemberService memberService;

	@Autowired
	public AdminUserController(MemberService memberService) {
		this.memberService = memberService;
	}
	
	@GetMapping(value = "/user")
	public ResponseEntity<?> userList() {
		logger.debug("userList call");
		try {
			List<MemberDto> list = memberService.listMember(null);
			if(list != null && !list.isEmpty()) {
				return new ResponseEntity<List<MemberDto>>(list, HttpStatus.OK);
//				return new ResponseEntity<List<MemberDto>>(HttpStatus.NOT_FOUND);
			} else {
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
			}
		} catch (Exception e) {
			return exceptionHandling(e);
		}
		
	}
	
	@PostMapping(value = "/user")
	public ResponseEntity<?> userRegister(@RequestBody MemberDto memberDto) {
		logger.debug("userRegister memberDto : {}", memberDto);
		try {
			memberService.joinMember(memberDto);
			List<MemberDto> list = memberService.listMember(null);
			return new ResponseEntity<List<MemberDto>>(list, HttpStatus.OK);
//			return new ResponseEntity<Integer>(cnt, HttpStatus.CREATED);
		} catch (Exception e) {
			return exceptionHandling(e);
		}
		
	}
	
	@GetMapping(value = "/user/{userid}")
	public ResponseEntity<?> userInfo(@PathVariable("userid") String userId) {
		logger.debug("userInfo userid : {}", userId);
		try {
			MemberDto memberDto = memberService.getMember(userId);
			if(memberDto != null)
				return new ResponseEntity<MemberDto>(memberDto, HttpStatus.OK);
			else
				return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
		} catch (Exception e) {
			return exceptionHandling(e);
		}
	}
	
	@PutMapping(value = "/user")
	public ResponseEntity<?> userModify(@RequestBody MemberDto memberDto) {
		logger.debug("userModify memberDto : {}", memberDto);
		try {
			memberService.updateMember(memberDto);
			List<MemberDto> list = memberService.listMember(null);
			return new ResponseEntity<List<MemberDto>>(list, HttpStatus.OK);
		} catch (Exception e) {
			return exceptionHandling(e);
		}
	}
	
	@DeleteMapping(value = "/user/{userid}")
	public ResponseEntity<?> userDelete(@PathVariable("userid") String userId) {
		logger.debug("userDelete userid : {}", userId);
		try {
			memberService.deleteMember(userId);
			List<MemberDto> list = memberService.listMember(null);
			return new ResponseEntity<List<MemberDto>>(list, HttpStatus.OK);
		} catch (Exception e) {
			return exceptionHandling(e);
		}
	}

	private ResponseEntity<String> exceptionHandling(Exception e) {
		e.printStackTrace();
		return new ResponseEntity<String>("Error : " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
