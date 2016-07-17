import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { Document } from './document';
import { DocumentService } from './document.service';

@Component({
    selector: 'my-dashboard',
    templateUrl: 'app/dashboard.component.html',
    styleUrls: ['app/dashboard.component.css']
})

export class DashboardComponent implements OnInit {
    documents: Document[] = [];

    constructor(
        private router: Router,
        private documentService: DocumentService) {
    };
    ngOnInit() {
        this.documentService.getDocuments()
            .then(documents => this.documents = documents.slice(1, 5));
    };

    gotoDetail(document: Document) {
        let link = ['/detail', document.id];
        this.router.navigate(link);
    };
}
