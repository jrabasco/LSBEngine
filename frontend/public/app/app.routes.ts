import { provideRouter, RouterConfig }  from '@angular/router';
import { DocumentsComponent } from './documents.component';
import { DashboardComponent } from "./dashboard.component";
import { DocumentDetailComponent } from './document-detail.component';

const routes: RouterConfig = [
    {
        path: '',
        redirectTo: '/dashboard',
        pathMatch: 'full'
    },
    {
        path: 'dashboard',
        component: DashboardComponent
    },
    {
        path: 'documents',
        component: DocumentsComponent
    },
    {
        path: 'detail/:id',
        component: DocumentDetailComponent
    }
];

export const appRouterProviders = [
    provideRouter(routes)
];
