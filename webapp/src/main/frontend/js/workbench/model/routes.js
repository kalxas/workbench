/**
 * Libraries
 */
import pathToRegexp from 'path-to-regexp';

/**
 * Components
 */
import { ResourceExplorerSidebar } from '../components/views/resource/explorer';
import { ProcessDesignerSidebar } from '../components/views/process/designer';
import { ExecutionViewerSidebar } from '../components/views/execution/viewer';

/**
 * Model
 */
import * as Roles from './role';

/**
 * Routes for utility pages
 */
const Login = '/login';
const Register = '/register';
const ResetPassword = '/reset-password';

export const Pages = {
  Login,
  Register,
  ResetPassword,
};

/**
 * Static routes
 */

const Dashboard = '/dashboard';
const Profile = '/profile';
const Settings = '/settings';

const ResourceExplorer = '/resource/explore';
const ResourceRegistration = '/resource/register';

const ProcessExplorer = '/process/explore';
const ProcessExecutionExplorer = '/process/execution/explore';

const RecipeExplorer = '/recipe/explore';

const SchemaExplorer = '/tools/schema/explore';

const UserManager = '/admin/user-manager';
const EventViewer = '/admin/event-viewer';

export const StaticRoutes = {
  Dashboard,
  Profile,
  Settings,
  ResourceExplorer,
  ResourceRegistration,
  ProcessExplorer,
  ProcessExecutionExplorer,
  RecipeExplorer,
  SchemaExplorer,
  UserManager,
  EventViewer,
};

/**
 * Dynamic routes
 */

const ResourceViewer = '/resource/view/:id/:version';

const ProcessDesignerCreate = '/workflow/designer';
const ProcessDesignerEdit = '/workflow/designer/:id';
const ProcessDesignerView = '/workflow/designer/:id/:version';

const ProcessExecutionViewer = '/workflow/designer/:id/:version/execution/:execution';
const ProcessExecutionMapViewer = '/workflow/designer/:id/:version/execution/:execution/map';

const SchemaDesigner = '/tools/schema/view/:id';


export const DynamicRoutes = {
  ResourceViewer,
  ProcessDesignerCreate,
  ProcessDesignerEdit,
  ProcessDesignerView,
  ProcessExecutionMapViewer,
  ProcessExecutionViewer,
  SchemaDesigner,
};

/**
 * Routes for error pages
 */

const Forbidden = '/error/403';
const NotFound = '/error/404';

export const ErrorPages = {
  Forbidden,
  NotFound,
};

/**
 * Default links
 */
const defaultLinks = [Dashboard, ResourceExplorer, ProcessExplorer];

const routes = {
  // Pages
  [Login]: {
    description: 'Login to workbench application',
  },
  [Register]: {
    description: 'Register a new account',
  },
  [ResetPassword]: {
    description: 'Reset user password',
  },
  // Static
  [Dashboard]: {
    description: 'Initial page',
    title: 'links.dashboard',
    defaultTitle: 'Dashboard',
    links: [ResourceExplorer, ProcessExplorer]
  },
  [Profile]: {
    description: 'Profile',
    title: 'links.profile',
    defaultTitle: 'Profile',
    links: defaultLinks
  },
  [Settings]: {
    description: 'Settings',
    title: 'links.settings',
    defaultTitle: 'Settings',
    links: defaultLinks
  },
  [ResourceExplorer]: {
    description: 'Browse registered resources',
    title: 'links.resource.explorer',
    defaultTitle: 'Resource Explorer',
    links: [Dashboard, ProcessExplorer, ResourceRegistration, ProcessDesignerCreate],
    contextComponent: ResourceExplorerSidebar,
  },
  [ResourceRegistration]: {
    description: 'Register a new resource',
    title: 'links.resource.registration',
    defaultTitle: 'Resource Registration',
    links: defaultLinks
  },
  [ProcessExplorer]: {
    description: 'Browse system processes',
    title: 'links.process.explorer',
    defaultTitle: 'Process Explorer',
    links: [Dashboard, ProcessDesignerCreate]
  },
  [ProcessExecutionExplorer]: {
    description: 'Browse workflow executions',
    title: 'links.process.execution.default',
    defaultTitle: 'Execution',
    links: [Dashboard, ProcessExplorer, ProcessDesignerCreate]
  },
  [RecipeExplorer]: {
    description: 'Browse recipes',
    title: 'links.recipe.explorer',
    defaultTitle: 'Recipe Explorer',
    links: defaultLinks
  },
  [SchemaExplorer]: {
    description: 'Browse schema mappings',
    title: 'links.tools.schema-explorer',
    defaultTitle: 'Schema Explorer',
    links: defaultLinks
  },
  [UserManager]: {
    description: 'Manage user accounts',
    title: 'links.admin.user-manager',
    defaultTitle: 'User Management',
    role: Roles.ADMIN,
    links: [Dashboard, EventViewer],
  },
  [EventViewer]: {
    description: 'Browse event logs',
    title: 'links.admin.event-viewer',
    defaultTitle: 'Event Viewer',
    role: Roles.ADMIN,
    links: [Dashboard, UserManager],
  },
  // Dynamic
  [ResourceViewer]: {
    description: 'View/Update an existing resource',
    title: 'links.resource.viewer',
    defaultTitle: 'Resource Viewer',
    links: defaultLinks
  },
  [ProcessDesignerCreate]: {
    description: 'Create a data integration workflow',
    title: 'links.process.designer.default',
    defaultTitle: 'Workflow Designer',
    links: defaultLinks,
    contextComponent: ProcessDesignerSidebar,
  },
  [ProcessDesignerEdit]: {
    description: 'Update a data integration workflow',
    title: 'links.process.designer.edit',
    defaultTitle: 'Edit',
    links: defaultLinks,
    contextComponent: ProcessDesignerSidebar,
  },
  [ProcessDesignerView]: {
    description: 'View a data integration workflow',
    title: 'links.process.designer.view',
    defaultTitle: 'View',
    links: defaultLinks,
    contextComponent: ProcessDesignerSidebar,
  },
  [ProcessExecutionViewer]: {
    description: 'View information about a workflow execution instance',
    title: 'links.process.execution.view',
    defaultTitle: 'Execution',
    links: defaultLinks,
    contextComponent: ProcessDesignerSidebar,
  },
  [SchemaDesigner]: {
    description: 'View/Update schema mappings',
    title: 'links.tools.schema-designer',
    defaultTitle: 'Schema Editor',
    links: defaultLinks
  },
  [ProcessExecutionMapViewer]: {
    description: 'View a POI dataset',
    title: 'links.process.execution.map-viewer',
    defaultTitle: 'Map Viewer',
    links: defaultLinks,
    contextComponent: ExecutionViewerSidebar,
  },
  // Error Pages
  [Forbidden]: {
    description: 'Forbidden',
  },
  [NotFound]: {
    description: 'Not Found',
  },
};

/**
 * Find a route by its path e.g. /Dashboard
 *
 * @export
 * @param {string} path - the route path
 * @returns the route properties
 */
export function getRoute(path) {
  const prop = matchRoute(path);

  if (routes.hasOwnProperty(prop)) {
    return routes[prop];
  }
  return null;
}

/**
 * Matches the given path to an existing route and returns the route or null
 * if no match is found
 *
 * @export
 * @param {any} path - the route path to match
 * @returns the route that matched the given path or null if no match is found
 */
export function matchRoute(path) {
  for (let route in routes) {
    let re = pathToRegexp(route);
    if (re.test(path)) {
      return route;
    }
  }

  return null;
}

/**
 * Build a path given a route and optional parameters
 *
 * @export
 * @param {string} path - The route name
 * @param {string[]|object} params - Optional parameters to bind
 */
export function buildPath(path, params) {
  let result = path || '/';

  if (params) {
    if (Array.isArray(params)) {
      let re = /:\w+/i;
      for (const value of params) {
        result = result.replace(re, value);
      }
    } else {
      let toPath = pathToRegexp.compile(path);
      result = toPath(params);
    }
  }
  return result;
}
