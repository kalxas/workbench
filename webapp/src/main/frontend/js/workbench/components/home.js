import * as React from 'react';
import { Switch, Route, Redirect } from 'react-router-dom';

import HTML5Backend from 'react-dnd-html5-backend';
import { DragDropContext } from 'react-dnd';

import { Container } from 'reactstrap';

import * as Roles from '../model/role';

import {
  StaticRoutes,
  DynamicRoutes,
  ErrorPages
} from '../model/routes';

import userPropType from '../model/prop-types/user';

import {
  Aside,
  Breadcrumb,
  Footer,
  Header,
  Sidebar,
} from './layout';

import SecureRoute from './helpers/secure-route';

import {
  Dashboard,
  EventViewer,
  HarvesterDataExplorer,
  ProcessDesigner,
  ProcessExecutionExplorer,
  ProcessExecutionMapViewer,
  ProcessExplorer,
  Profile,
  RecipeExplorer,
  ResourceExplorer,
  ResourceExport,
  ResourceMapViewer,
  ResourceRegistration,
  ResourceViewer,
  Settings,
  UserFileSystem,
  UserManager,
} from './views/';

/////////////////////////////////////////////////////////////////
//
//    // Header options
//    1. '.header-fixed'					- Fixed Header
//
//    // Sidebar options
//    1. '.sidebar-fixed'					- Fixed Sidebar
//    2. '.sidebar-hidden'				- Hidden Sidebar
//    3. '.sidebar-off-canvas'		- Off Canvas Sidebar
//    4. '.sidebar-minimized'			- Minimized Sidebar (Only icons)
//    5. '.sidebar-compact'			  - Compact Sidebar
//    6. '.sidebar-mobile-show'   - ??
//
//    // Aside options
//    1. '.aside-menu-fixed'			- Fixed Aside Menu
//    2. '.aside-menu-hidden'			- Hidden Aside Menu
//    3. '.aside-menu-off-canvas'	- Off Canvas Aside Menu
//
//    // Footer options
//    1. 'footer-fixed'						- Fixed footer
//
/////////////////////////////////////////////////////////////////

@DragDropContext(HTML5Backend)
class Home extends React.Component {
  constructor(props) {
    super(props);

    this._toggleSidebar = this._toggleSidebar.bind(this);
    this._styleSidebar = this._styleSidebar.bind(this);
    this._setAsideMenuVisibility = this._setAsideMenuVisibility.bind(this);
    this._setSideBarVisibility = this._setSideBarVisibility.bind(this);
    this._toggleAsideMenu = this._toggleAsideMenu.bind(this);
    this._styleAsideMenu = this._styleAsideMenu.bind(this);

    this.state = {
      sidebarOpen: true,
      sidebarStyle: 'fixed', // fixed, compact, minimized, off-canvas
      asideOpen: false,
      asideStyle: 'fixed', // fixed, off-canvas
    };
  }

  static propTypes = {
    user: userPropType,
  }

  _toggleSidebar() {
    this.setState({ sidebarOpen: !this.state.sidebarOpen });
  }

  _styleSidebar(style) {
    if (['fixed', 'compact', 'minimized', 'off-canvas', 'mobile-show'].indexOf(style) < 0) {
      console.warn('Ignoring unknown sidebar style: ' + style);
      return;
    }

    this.setState({ sidebarStyle: style });
  }

  _setSideBarVisibility(value) {
    this.setState({ sidebarOpen: value });
  }

  _setAsideMenuVisibility(value) {
    this.setState({ asideOpen: value });
  }

  _toggleAsideMenu() {
    this.setState({ asideOpen: !this.state.asideOpen });
  }

  _styleAsideMenu(style) {
    if (['fixed', 'off-canvas'].indexOf(style) < 0) {
      console.warn('Ignoring unknown aside-menu style: ' + style);
      return;
    }

    this.setState({ asideStyle: style });
  }

  render() {
    const { user } = this.props;

    const cssClasses = [
      'app',
      /* header-* */
      'header-fixed',
      /* sidebar-* */
      this.state.sidebarOpen ? null : 'sidebar-hidden',
      'sidebar-' + (this.state.sidebarStyle || 'fixed'),
      /* aside-menu-* */
      this.state.asideOpen ? null : 'aside-menu-hidden',
      'aside-menu-' + (this.state.asideStyle || 'fixed'),
    ];

    return (
      <div className={cssClasses.join(' ')}>
        <Header
          user={user}
          toggleSidebar={this._toggleSidebar}
          styleSidebar={this._styleSidebar}
          setAsideMenuVisibility={this._setAsideMenuVisibility}
          toggleAsideMenu={this._toggleAsideMenu}
          styleAsideMenu={this._styleAsideMenu}
        />
        <div className="app-body">
          <Route path="/" component={Sidebar} />
          <div className="main">
            <Route
              path="/"
              render={(props) =>
                <Breadcrumb {...props} setAsideMenuVisibility={this._setAsideMenuVisibility} setSidebarVisibility={this._setSideBarVisibility} />}
            />
            <Container fluid className="slipo-container">
              <Switch>
                <Redirect from="/" to={StaticRoutes.Dashboard} exact />
                {/* Dynamic */}
                <Route path={DynamicRoutes.ResourceMapViewer} component={ResourceMapViewer} />
                <Route path={DynamicRoutes.ResourceViewer} component={ResourceViewer} />
                <Route path={DynamicRoutes.ProcessDesignerView} component={ProcessDesigner} exact />
                <SecureRoute path={DynamicRoutes.ProcessDesignerEditTemplate} component={ProcessDesigner} exact roles={[Roles.ADMIN, Roles.AUTHOR]} />
                <SecureRoute path={DynamicRoutes.ProcessDesignerEdit} component={ProcessDesigner} exact roles={[Roles.ADMIN, Roles.AUTHOR]} />
                <SecureRoute path={DynamicRoutes.ProcessDesignerCreate} component={ProcessDesigner} exact roles={[Roles.ADMIN, Roles.AUTHOR]} />
                <Route path={DynamicRoutes.ProcessExecutionViewer} component={ProcessDesigner} exact />
                <Route path={DynamicRoutes.ProcessExecutionMapViewer} component={ProcessExecutionMapViewer} />
                {/* Static */}
                <Route path={StaticRoutes.Dashboard} component={Dashboard} />
                <Route path={StaticRoutes.UserFileSystem} component={UserFileSystem} />
                <SecureRoute path={StaticRoutes.HarvesterDataExplorer} component={HarvesterDataExplorer} roles={[Roles.ADMIN]} />
                <Route path={StaticRoutes.Profile} component={Profile} />
                <Route path={StaticRoutes.Settings} component={Settings} />
                <Route path={StaticRoutes.ResourceExplorer} component={ResourceExplorer} />
                <SecureRoute path={StaticRoutes.ResourceExport} component={ResourceExport} roles={[Roles.ADMIN, Roles.AUTHOR]} />
                <SecureRoute path={StaticRoutes.ResourceRegistration} component={ResourceRegistration} roles={[Roles.ADMIN, Roles.AUTHOR]} />
                <Route path={StaticRoutes.ProcessExplorer} component={ProcessExplorer} />
                <Route path={StaticRoutes.ProcessExecutionExplorer} component={ProcessExecutionExplorer} />
                <Route path={StaticRoutes.RecipeExplorer} component={RecipeExplorer} />
                <SecureRoute path={StaticRoutes.UserManager} component={UserManager} roles={[Roles.ADMIN]} />
                <SecureRoute path={StaticRoutes.EventViewer} component={EventViewer} roles={[Roles.ADMIN, Roles.DEVELOPER]} />
                {/* Default */}
                <Redirect push={true} to={ErrorPages.NotFound} />
              </Switch>
            </Container>
          </div>
          <Aside />
        </div>
        <Footer />
      </div>
    );
  }
}

export default Home;
