<form id="form_update_profile" action="${request.contextPath}?_mode=update" name="form_update_profile" class="form-container " method="POST" enctype="multipart/form-data">
	<input type="hidden" value="" name="_FORM_META_ORIGINAL_ID" />
	<input type="hidden" value="true" name="_form_update_profile_SUBMITTED" />

	<div id="section_file_upload" class="form-section  section_">
		<div class="form-section-title"><span>Section</span></div>
		<div id="" class="form-column" style="width: 100%" >
			<div class="form-cell" >
				<label class="label"><span class="form-cell-validator"> * </span></label>
				<div class="form-fileupload">
				</div>
			</div>
		</div>
		<div style="clear:both"></div>
	</div>
	<div id="section-actions" class="form-section no_label section_">
        <div class="form-section-title"></div>
            <div id="" class="form-column form-column-horizontal" style="width: ">
                <div class="form-cell">
                    <input id="submit" name="submit" class="form-button" type="submit" value="Import">
                </div>
            </div>
        </div>
        <div style="clear:both"></div>
    </div>
</form>
<script>
	$(function(){
		$("#section-actions button, #section-actions input").click(function(){
			$.blockUI({
				css: {
					border: 'none',
					padding: '15px',
					backgroundColor: '#000',
					'-webkit-border-radius': '10px',
					'-moz-border-radius': '10px',
					opacity: .3,
					color: '#fff'
				}, message : "<h1>Please wait...</h1>"
			});

			return true;
		});
	});
</script>