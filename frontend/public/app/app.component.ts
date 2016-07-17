import { Component } from '@angular/core';
import { ROUTER_DIRECTIVES } from '@angular/router';
import { DocumentService }     from './document.service';

@Component({
    selector: 'my-app',
    templateUrl: 'app/app.component.html',
    styleUrls: ['app/app.component.css'],
    directives: [ROUTER_DIRECTIVES],
    providers: [DocumentService]
})
export class AppComponent {
    title = 'Browse Documents';
}
