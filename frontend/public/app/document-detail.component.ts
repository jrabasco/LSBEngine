import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Document } from './document';
import { DocumentService } from './document.service';

@Component({
    selector: 'document-detail',
    templateUrl: 'app/document-detail.component.html',
    styleUrls: ['app/document-detail.component.css']
})
export class DocumentDetailComponent implements OnInit, OnDestroy {
    @Input() document: Document;

    sub: any;

    constructor(
        private documentService: DocumentService,
        private route: ActivatedRoute) {
    };

    goBack() {
        window.history.back();
    };

    ngOnInit() {
        this.sub = this.route.params.subscribe(params => {
            let id = +params['id'];
            this.documentService.geDocument(id)
                .then(document => this.document = document);
        });
    }

    ngOnDestroy() {
        this.sub.unsubscribe();
    }
}
