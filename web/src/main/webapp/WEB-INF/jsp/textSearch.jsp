<script type="text/x-handlebars" data-template-name="textSearch">
  <div class="textsearch clearfix">
          <div class="input clearfix">
            {{render "searchInput"}}
          </div>
          {{#if controllers.searchResults.model.total}}
            <div class="clearfix">
              {{render "searchResults"}}
            </div>
            <div class="clearfix">
              {{render "pages"}}
            </div>
          {{/if}}
      </div>
    </div>
  </div>
</script>

<script type="text/x-handlebars" data-template-name="searchInput">
    <div class="row">
      <div class="col-lg-12">
        {{view Bootstrap.Forms.TextField valueBinding="query" label="" placeholder="#springMessage('input.field.refset.concept.search')"}}
      </div>
    </div>
</script>

<script type="text/x-handlebars" data-template-name="searchResults" >
  <div class="row">
    <div class="col-lg-12">
      <ul class="list-group results">
        {{#each model.concepts}}
          <a {{action 'click' this}} href="#" class="list-group-item result">
            <h4 {{bind-attr class=stylingClass}}>
              <div class="row">
                <div class="col-lg-10">{{title}}</div>
                <div class="col-lg-2 identifier pull-right">{{id}}</div>
              </div>
            </h4>
            <div class="property">
              <label>Effective</label>
              <span class="value">{{effectiveTime}}</span>
            </div>
          </a>
        {{/each}}
      </ul>
    </div>
  </div>
</script>

<script type="text/x-handlebars" data-template-name="pages">
  {{#if shouldDisplayNavigation}}
    <ul class="pagination"  {{bind-attr style=displayWidthStyling}}>
      <li class="inactive index"><a href="#" {{action "firstPage"}} style="border-bottom-left-radius: 4px;border-top-left-radius: 4px;margin-left: 0;"><small>&lt;&lt;</a></small></li>
      <li class="inactive index"><a href="#" {{action "previousPage"}}><small>&lt;</small></a></li>
      {{#each page in model itemViewClass="Em.View"}}
        {{#if page.active}}
          <li class="active index"><a href="#" class="number" {{action "pageRequest" page}}>{{page.index}}</a></li>
        {{else}}
          <li class="inactive index"><a href="#" class="number" {{action "pageRequest" page}}>{{page.index}}</a></li>
        {{/if}}
      {{/each}}
      <li class="inactive index"><a href="#" {{action "nextPage"}}><small>&gt;</small></a></li>
      <li class="inactive index"><a href="#" style="border-bottom-right-radius: 4px; border-top-right-radius: 4px;" {{action "lastPage"}}><small>&gt;&gt;</small></a></li>
    </ul>
  {{/if}}
</script>
]]#