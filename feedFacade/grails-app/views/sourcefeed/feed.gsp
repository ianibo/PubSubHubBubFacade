<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Source Feed : .....</title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  <div class="container-fluid">
    <div class="row">
      <div class="container-fluid">
        <h1>[${feed.id}] <a href="${feed.baseUrl}">${feed.uriname} / ${feed.name}</a></h1>

        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">Feed Info</h3>
          </div>
          <div class="panel-body form-horizontal">
            <div class="form-group">
              <label class="col-sm-2 control-label">URL</label>
              <div class="col-sm-10"><p class="form-control-static">${feed.baseUrl}</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Semantic Status</label>
              <div class="col-sm-10"><p class="form-control-static"><strong>${feed.capAlertFeedStatus}</strong> (operating|testing|OTHER) - only operating and testing feeds will be harvested</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Harvester Control Status</label>
              <div class="col-sm-10"><p class="form-control-static"><strong>${feed.status}</strong> (in-process|paused)</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Last fetch status</label>
              <div class="col-sm-10"><p class="form-control-static"><strong>${feed.feedStatus}</strong>(Last completed=<g:formatDate date="${new Date(feed.lastCompleted)}"/>, highest timestamp=<g:formatDate date="${new Date(feed.highestTimestamp?:0)}"/>)</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Health Last Hour</label>
              <div class="col-sm-10"><p backgroundColor="" class="form-control-static">
                ${feed.latestHealth}%
              </p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Last Error (If any)</label>
              <div class="col-sm-10"><p class="form-control-static"><strong>${feed.lastError}</strong></p></div>
            </div>
          </div>
        </div>


        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">Feed Activity - last 24 hours</h3>
          </div>
          <div class="panel-body form-horizontal">
            <g:set var="stats" value="${feed.getHistogramLastDay()}"/>
            <table class="table">
              <thead>
                <tr>
                  <th class="col-md-1">Hour (UTC)&nbsp;</th>
                  <g:each in="${stats}" var="h">
                    <th>${h.hour}</th>
                  </g:each>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th>Error Count &nbsp;</th>
                  <g:each in="${stats}" var="h">
                    <td>${h.errorCount}</td>
                  </g:each>
                </tr>
                <tr>
                  <th>Success Count &nbsp;</th>
                  <g:each in="${stats}" var="h">
                    <td>${h.successCount}</td>
                  </g:each>
                </tr>
                <tr>
                  <th>New Entries</th>
                  <g:each in="${stats}" var="h">
                    <td>${h.newEntryCount}</td>
                  </g:each>
                </tr>
                <tr>
                  <th>Health</th>
                  <g:each in="${stats}" var="h">
                    <td>
                      ${h.health}%
                    </td>
                  </g:each>
                </tr>
              </tbody>
            </table>
          </div>
        </div>


        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">Recent Issues</h3>
          </div>
          <div class="panel-body form-horizontal">
            <g:set var="latest_issues" value="${feed.latestIssues(5)}"/>
            <g:if test="${latest_issues.size() > 0 }">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Key</th>
                    <th>Message</th>
                    <th>First Seen</th>
                    <th>Last Seen</th>
                    <th>Repeated</th>
                  </tr>
                </thead>
                <tbody>
                  <g:each in="${latest_issues}" var="li">
                    <tr>
                      <td>${li.key} </td>
                      <td>${li.message} </td>
                      <td><g:formatDate date="${new Date(li.firstSeen)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                      <td><g:formatDate date="${new Date(li.lastSeen)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                      <td>${li.occurrences} </td>
                    </tr>
                  </g:each>
                </tbody>
              </table>
            </g:if>
            <g:set var="feed_flags" value="${feed.getFlags()}"/>
            <g:if test="${feed_flags.size() > 0 }">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Flag</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>First Seen</th>
                    <th>Last Seen</th>
                    <th>Expiry</th>
                  </tr>
                </thead>
                <tbody>
                  <g:each in="${feed_flags}" var="flag">
                    <tr>
                      <td>${flag.definition.code} </td>
                      <td>${flag.definition.name} </td>
                      <td>${flag.definition.type} </td>
                      <td><g:formatDate date="${new Date(flag.firstSeen)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                      <td><g:formatDate date="${new Date(flag.lastSeen)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/></td>
                      <td>
                        <g:formatDate date="${new Date(flag.expiryTime)}" format="yyyy-MM-dd HH:mm:ssz" timeZone="${UTCZONE}"/>
                        ( ${flag.expiryTime - System.currentTimeMillis()} ) 
                      </td>
                    </tr>
                    <tr>
                      <td colspan="6">${flag.definition.advice}</td>
                    </tr>
                  </g:each>
                </tbody>
              </table>
            </g:if>
          </div>
        </div>


        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">Recent Entries</h3>
          </div>
          <div class="panel-body form-horizontal">

            <table class="table table-striped well">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Entry</th>
                </tr>
              </thead>
              <tbody>
                <g:each in="${latestEntries}" var="f" >
                  <tr>
                    <td><g:formatDate date="${new java.util.Date(f?.entryTs)}" format="yyyy-MM-dd'T'HH:mm:ss.SSS" /></td>
                    <td>
                      <strong><a href="${f.link}">${f.title}</a></strong>
                      <p>${f.description}</p>
                      <div class="container-fluid">
                        <div class="row">
                          <div class="col-md-6">
                            ${f.entry}
                          </div>
                          <div class="col-md-6">
                            ${f.entryAsJson}
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>
                </g:each>
              </tbody>
            </table>

          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>
