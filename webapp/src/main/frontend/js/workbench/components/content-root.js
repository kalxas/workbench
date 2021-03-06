import _ from 'lodash';
import * as React from 'react';
import * as ReactRedux from 'react-redux';
import { bindActionCreators } from 'redux';
import { Switch, Route, Redirect } from 'react-router-dom';

import {
  toast,
  ToastContainer
} from 'react-toastify';

import { Pages, StaticRoutes, ErrorPages } from '../model/routes';
import userPropType from '../model/prop-types/user';
import { resize } from '../ducks/ui/viewport';
import { getFilesystem } from '../ducks/config';

import {
  LoginForm,
  Page403,
  Page404,
} from './pages/';

import {
  Placeholder,
} from './helpers/';

import {
  message,
} from '../service';

import Home from './home';

//
// Presentational component
//
class ContentRoot extends React.Component {

  constructor(props) {
    super(props);
  }

  componentWillMount() {
    if (this.props.user != null) {
      this._getFileSystem();
    }
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.user == null && nextProps.user != null) {
      this._getFileSystem();
    }
  }

  componentDidMount() {
    this._listener = _.debounce(this._setViewport.bind(this), 150);
    window.addEventListener('resize', this._listener);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this._listener);
  }

  _getFileSystem() {
    this.props.getFilesystem()
      .catch(() => {
        message.error('error.FILESYSTEM_LOAD', 'fa-folder');
      });
  }

  _setViewport() {
    this.props.resize(
      document.documentElement.clientWidth,
      document.documentElement.clientHeight
    );
  }

  render() {
    let authenticated = (this.props.user != null);
    let routes;

    if (!authenticated) {
      routes = (
        <Switch>
          <Route path={Pages.Login} name="login" component={LoginForm} />
          <Route path={Pages.ResetPassword} name="reset-password" component={Placeholder} />
          <Redirect push={true} to={Pages.Login} />
        </Switch>
      );
    } else {
      routes = (
        <Switch>
          {/* Handle errors first */}
          <Route path={ErrorPages.Forbidden} component={Page403} exact />
          <Route path={ErrorPages.NotFound} component={Page404} exact />
          {/* Redirect for authenticated users. Navigation after a successful login operation
              occurs after the component hierarchy is rendered due to state change and causes
              /error/404 to render */}
          <Redirect from={Pages.Login} to={StaticRoutes.Dashboard} exact />
          <Redirect from={Pages.Register} to={StaticRoutes.Dashboard} exact />
          <Redirect from={Pages.ResetPassword} to={StaticRoutes.Dashboard} exact />
          {/* Default component */}
          <Route
            path="/"
            name="home"
            component={() => (
              <Home
                user={this.props.user}
              />
            )}
          />
        </Switch>
      );
    }

    return (
      <div>
        <ToastContainer
          className="slipo-toastify"
          position="top-right"
          type="default"
          autoClose={5000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          pauseOnHover
        />
        {routes}
      </div>
    );
  }
}

ContentRoot.propTypes = {
  user: userPropType
};

//
// Container component
//

const mapStateToProps = (state) => ({
  user: state.user.profile,
});

const mapDispatchToProps = (dispatch) => bindActionCreators({
  getFilesystem,
  resize,
}, dispatch);

export default ReactRedux.connect(mapStateToProps, mapDispatchToProps)(ContentRoot);
