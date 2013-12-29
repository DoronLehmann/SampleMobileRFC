var path = "/irj/servlet/prt/portal/prtroot/SampleMobileRFC.SampleMobileRFCApp?";

jQuery(document).ready(function(){
	jQuery("#listViewContent").hide();
	jQuery("#listView").hide();
	jQuery("#search").show();
	jQuery("#searchButton").click(function(){
		jQuery.mobile.showPageLoadingMsg();
		var searchPath = path + "action=search&term=" + jQuery("#searchField").val();
		jQuery.ajax({
			url: searchPath,
			dataType: 'json',
			success: function(data){
				renderList(data);
			},
			error: function(jqXHR, textStatus, errorThrown){
				renderList("[]");
			}
		});
	});
	jQuery("#backBtn1").click(function(){
		jQuery.mobile.showPageLoadingMsg();	
		jQuery("#listView").hide();
		jQuery("#search").show();
		jQuery.mobile.hidePageLoadingMsg();	
	});
});

function renderList(results){
	var result = "";
	var ul = jQuery("#collectionList").html("");
	if (results){
		for ( var i = 0; i < results.length; i++){
			var currResult = results[i];
			result = '<li id=liId' + i + ' data-role="none">' +
			'<span class="RFCListText">' +
			'<h3> Name: ' + currResult["name"] + '</h3>'+
			'<span> Organization : ' + currResult["org"] + '</span>' +
			'</span></li>';
			ul.append( result );
			bindOnClickLi( jQuery("#liId" + i ), currResult );
		}
		if(results.length == 0 ){
			result = '<li id=liId' + i + ' data-role="none">'+
			'<span class="entryItemText">'+
			'<h3 style="text-align: center;">No Items To Display</h3>'+
			'</span>'+
			"</li>";
			ul.append( result );
		}
	}	
	jQuery("#collectionList").listview('refresh');
	jQuery("#listView").show();
	jQuery("#search").hide();
	jQuery.mobile.hidePageLoadingMsg();	
}

var bindOnClickLi = function bindOnClickLi( button, liBtn ){
	jQuery( button ).unbind('click').bind('click',function (){
		jQuery.mobile.showPageLoadingMsg();	
		var ul = jQuery("#propertiesList");
		ul.find('li').remove();
		var itemPath = path + 'action=getemploye&id=' +  liBtn["id"];
		jQuery.ajax({
			url: itemPath,
			dataType: 'json',
			success: function(data){
				var li = '<li><div class="textWrapper"><span class="RFCListText"> Name : </span><span style="font-weight: 100;float:right;">' +  data[0]["FIRSTNAME"]+ " " + data[0]["LAST_NAME"]+ '</span></span></div></li>' +
				'<li><div class="textWrapper"><span class="RFCListText">Birth Date : </span><span style="font-weight: 100;float:right;">' +  data[0]["BIRTHDATE"] + '</span></span></div></li>' +
				'<li><div class="textWrapper"><span class="RFCListText">E-Mail : </span><span style="font-weight: 100;float:right;"><a href="mailto:' + data[0]["USRID_LONG"] + '">'+ data[0]["USRID_LONG"] + '</a></span></span></div></li>';
				ul.append(li);
				goToContent();
			},
			error: function(jqXHR, textStatus, errorThrown){
				var li = '<li><div class="textWrapper"><span class="RFCListText">Error....</span></span></div></li>'; 
				ul.append(li);
				goToContent();
			}
		});
	});
}

var goToContent = function goToContent(){
	jQuery("#listView").toggle();
	jQuery("#listViewContent").toggle();
	jQuery("#detailsBackBtn").unbind('click').bind('click',backOnClick);
	jQuery.mobile.hidePageLoadingMsg();	
}

var backOnClick = function backOnClick(){
	jQuery("#listViewContent").toggle();
	jQuery("#listView").toggle();
}
