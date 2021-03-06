import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { NgOption } from '@ng-select/ng-select';
import { Select2Data } from 'ng-select2-component/lib/select2-utils';
import { UserActivityService } from '../../common/services/user-activity.service';
import { ToastrService } from 'ngx-toastr';
import { NgxSpinnerService } from "ngx-spinner";
import * as _moment from 'moment';
import { LoaderService } from "../../common/services/loader.service";
import { Title } from '@angular/platform-browser';
import { tickStep } from 'd3';
import { CommonService } from 'src/app/common/services/common.service';
import { Router } from '@angular/router';
import { MessageObj } from 'src/app/common/constants/constants';

@Component({
  selector: 'app-useractivity',
  templateUrl: './useractivity.component.html',
  styleUrls: ['./useractivity.component.css']
})
export class UseractivityComponent implements OnInit {
	moment = (_moment as any).default ? (_moment as any).default : _moment;
	userActivityData: any;
	startDate: any;
	endDate: any;
	currentDate:any = this.moment().format('YYYY/MM/DD HH:mm');
	type = [];
	username = [];
	showFilterBlock: boolean = false;
	showStartDateFilter: boolean = false;
	showEndDateFilter: boolean = false;
	showTypeFilter: boolean = false;
	showUsernameFilter: boolean = false;
	userDrowdonList = [];
	typeDropdownList = [];
	userActivityForm: FormGroup;
	typeData: NgOption[];
	userId = [];
	typeValue:any;
	usernameValue:string;

  constructor( private userActivityService:UserActivityService,
		private toastr: ToastrService,
		private formBuilder:FormBuilder,
		private loaderService: LoaderService,
		private titleService : Title	,
		private commonService:CommonService,
		private router:Router,
		private toaster:ToastrService		
		) { 

			if(!this.commonService.hasPermission('menu_user_activity')){
				this.toaster.error(MessageObj.unauthorised);  
				 this.router.navigate(["/home"]);
				}
		}

  ngOnInit() {
		this.titleService.setTitle('OPEN KILDA - User Activity');
    this.loaderService.show(MessageObj.loading_user_activity);
    this.callActivityService();
    this.callUserDropdownService();
    this.callTypeDropdownService();
  	this.userActivityForm = this.formBuilder.group({
      typeControl: [''],
      usernameControl: [''],
      toDateControl: [''],
      fromDateControl: ['']
    });
  }

  callActivityService(){
    
    this.userActivityService.getUserActivityList().subscribe((data : any) =>{
    data = data.sort(function(a,b){
    return b.activityTime - a.activityTime
    });
    this.userActivityData = data;
    this.loaderService.hide();
     },error=>{
       this.loaderService.hide();
       this.toastr.error(MessageObj.no_user_activity,'Error');
     });
  }

  callUserDropdownService(){
    
    this.userActivityService.getUserDropdownList().subscribe((data : Array<object>) =>{
    this.userDrowdonList = data;
     },error=>{
       this.toastr.error("No user dropdown data",'Error');
     });
  }

  callTypeDropdownService(){
    
    this.userActivityService.getTypeDropdownList().subscribe((data : Array<object>) =>{
    this.typeDropdownList = data;
     },error=>{
       this.toastr.error("No type dropdown data",'Error');
     });
  }

  onStartDateChange(event){
	  this.startDate = event.target.value;
	  if(this.moment(new Date(this.startDate)).isAfter(this.moment(new Date(this.endDate)))){
		this.toastr.error('Start Date must me less than End Date',"Error");
		this.startDate = null;
		event.target.value = '';
		return;
	}else if(this.moment(new Date(this.startDate)).isAfter(this.moment(new Date()))){
		this.toastr.error('Start Date must me less than current Date and Time',"Error");
		this.startDate = null;
		event.target.value = '';
		return;
	}
  	if(this.startDate !== ''){
  		this.showStartDateFilter = true;
  	}
  	else{
  		this.showStartDateFilter = false;
  	}
  	this.checkAllFilters();
  }

  onEndDateChange(event){
	  this.endDate = event.target.value;
	  if(this.moment(new Date(this.startDate)).isAfter(this.moment(new Date(this.endDate)))){
		  this.toastr.error('End Date must me greater than Start Date',"Error");
		  this.endDate = null;
		  event.target.value = '';
		  return;
	  }
  	if(this.endDate !== ''){
  		this.showEndDateFilter = true;
  	}
  	else{
  		this.showEndDateFilter = false;
  	}
  	this.checkAllFilters();
  }

  onTypeInputChange(event){
  	this.type = [];
  	if(event.length > 0){
  	for(let i = 0 ; i<event.length; i++){
  		this.type.push(event[i].name);
  	}}
  	this.typeValue= this.type.toString();
  	if(this.type.toString() !== ''){
  		this.showTypeFilter = true;
  	}
  	else{
  		this.showTypeFilter = false;
  	}
  	this.checkAllFilters();
  }

  onUsernameInputChange(event){
		this.userId = [];
		this.username = [];
  		if(event.length > 0){
	  	for(let i = 0 ; i<event.length ; i++){
  			this.userId.push(event[i].user_id);
	  		this.username.push(event[i].user_name);
  		}}

  	this.usernameValue=this.username.toString();
  	if(this.username.toString() !== ''){
  		this.showUsernameFilter = true;
  	}
  	else{
  		this.showUsernameFilter = false;
  	}
  	this.checkAllFilters();
  }

  checkAllFilters(){
  	if(this.showUsernameFilter === false && this.showTypeFilter === false &&
  	 this.showEndDateFilter === false && this.showStartDateFilter === false ){
  		this.showFilterBlock = false;
  	}
  	else{
  		this.showFilterBlock = true;
  	}
  }

  getFilteredDetails(){
    this.loaderService.show(MessageObj.loading_user_activity);
  	this.userActivityService.getFilteredUserActivityList(this.userId, this.type, this.startDate, this.endDate).subscribe((data : any) =>{
    data = data.sort(function(a,b){
    return b.activityTime - a.activityTime
    })  
    this.loaderService.hide();
   this.userActivityData = data;
     },error=>{
       this.loaderService.hide();
       this.toastr.error(MessageObj.no_user_activity,'Error');

     });
  }

  removeStartDateFilter(){
  	this.startDate='';
    this.userActivityForm.controls["fromDateControl"].setValue("");
  	this.showStartDateFilter = false;
  	this.checkAllFilters();
  }

  removeEndDateFilter(){
  	this.endDate='';
  	this.showEndDateFilter = false;
    this.userActivityForm.controls["toDateControl"].setValue("");
  	this.checkAllFilters();
  }

  removeUsernameFilter(){
  	this.userId=[];
    this.userActivityForm.controls["usernameControl"].setValue([]);
  	this.showUsernameFilter = false;
  	this.checkAllFilters();
  }

  removeTypeFilter(){
  	this.type=[];
    this.userActivityForm.controls["typeControl"].setValue([]);
   	this.showTypeFilter = false;
  	this.checkAllFilters();
  }

  setToCurrentDate(){
  	this.endDate = this.moment().format('YYYY/MM/DD HH:mm');
  	this.userActivityForm.controls["toDateControl"].setValue(this.endDate);
  	let event = { "target" : { 'value': this.endDate}} ;
  	this.onEndDateChange(event);
  }
}
