/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.gui.main.dao.compensation.active;

import com.google.common.util.concurrent.FutureCallback;
import io.bitsquare.common.UserThread;
import io.bitsquare.dao.compensation.CompensationRequest;
import io.bitsquare.dao.compensation.CompensationRequestManager;
import io.bitsquare.gui.Navigation;
import io.bitsquare.gui.common.view.ActivatableView;
import io.bitsquare.gui.common.view.FxmlView;
import io.bitsquare.gui.components.InputTextField;
import io.bitsquare.gui.components.TableGroupHeadline;
import io.bitsquare.gui.main.MainView;
import io.bitsquare.gui.main.dao.DaoView;
import io.bitsquare.gui.main.dao.compensation.CompensationRequestDisplay;
import io.bitsquare.gui.main.dao.voting.VotingView;
import io.bitsquare.gui.main.dao.voting.vote.VoteView;
import io.bitsquare.gui.main.overlays.popups.Popup;
import io.bitsquare.gui.util.BSFormatter;
import io.bitsquare.gui.util.Layout;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.SortedList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;
import static io.bitsquare.gui.util.FormBuilder.addButtonAfterGroup;
import static io.bitsquare.gui.util.FormBuilder.addLabel;

@FxmlView
public class ActiveCompensationRequestView extends ActivatableView<SplitPane, Void> {

    TableView<CompensationRequest> tableView;
    private InputTextField nameTextField, titleTextField, categoryTextField, descriptionTextField, linkTextField,
            startDateTextField, endDateTextField, requestedBTCTextField, btcAddressTextField;

    private final CompensationRequestManager compensationRequestManger;
    private final BSFormatter formatter;
    private Navigation navigation;
    private FundCompensationRequestWindow fundCompensationRequestWindow;
    private BSFormatter btcFormatter;
    private SortedList<CompensationRequest> sortedList;
    private Subscription selectedCompensationRequestSubscription;
    private CompensationRequestDisplay CompensationRequestDisplay;
    private GridPane gridPane;
    private Button fundButton;
    private Button voteButton;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    private ActiveCompensationRequestView(CompensationRequestManager compensationRequestManger, BSFormatter formatter, Navigation navigation,
                                          FundCompensationRequestWindow fundCompensationRequestWindow, BSFormatter btcFormatter) {
        this.compensationRequestManger = compensationRequestManger;
        this.formatter = formatter;
        this.navigation = navigation;
        this.fundCompensationRequestWindow = fundCompensationRequestWindow;
        this.btcFormatter = btcFormatter;
    }

    @Override
    public void initialize() {
        root.setDividerPositions(0.3, 0.7);
        root.setStyle("-fx-background-insets: 0, 0 0 0 0");
        AnchorPane topAnchorPane = new AnchorPane();
        root.getItems().add(topAnchorPane);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        AnchorPane.setBottomAnchor(gridPane, 10d);
        AnchorPane.setRightAnchor(gridPane, 10d);
        AnchorPane.setLeftAnchor(gridPane, 10d);
        AnchorPane.setTopAnchor(gridPane, 10d);
        topAnchorPane.getChildren().add(gridPane);

        TableGroupHeadline header = new TableGroupHeadline("Active compensation request");
        GridPane.setRowIndex(header, 0);
        GridPane.setMargin(header, new Insets(0, -10, -10, -10));
        gridPane.getChildren().add(header);
        header.setMinHeight(20);
        header.setMaxHeight(20);

        tableView = new TableView<>();
        GridPane.setRowIndex(tableView, 1);
        GridPane.setMargin(tableView, new Insets(5, -15, -10, -10));
        GridPane.setVgrow(tableView, Priority.ALWAYS);
        GridPane.setHgrow(tableView, Priority.ALWAYS);
        gridPane.getChildren().add(tableView);

        // tableView.setMinHeight(100);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("No transactions available"));
        sortedList = new SortedList<>(compensationRequestManger.getObservableCompensationRequestsList());
        tableView.setItems(sortedList);
        setColumns();
    }

    @Override
    protected void activate() {
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        sortedList.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.getSelectionModel().clearSelection();
        selectedCompensationRequestSubscription = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), this::onSelectCompensationRequest);
    }

    @Override
    protected void deactivate() {
        sortedList.comparatorProperty().unbind();
        selectedCompensationRequestSubscription.unsubscribe();
    }

    private void onSelectCompensationRequest(CompensationRequest compensationRequest) {
        if (compensationRequest != null) {
            if (CompensationRequestDisplay == null) {
                ScrollPane scrollPane = new ScrollPane();
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setMinHeight(100);
                root.getItems().add(scrollPane);

                AnchorPane bottomAnchorPane = new AnchorPane();
                scrollPane.setContent(bottomAnchorPane);

                gridPane = new GridPane();
                gridPane.setHgap(5);
                gridPane.setVgap(5);
                ColumnConstraints columnConstraints1 = new ColumnConstraints();
                columnConstraints1.setHalignment(HPos.RIGHT);
                columnConstraints1.setHgrow(Priority.SOMETIMES);
                columnConstraints1.setMinWidth(140);
                ColumnConstraints columnConstraints2 = new ColumnConstraints();
                columnConstraints2.setHgrow(Priority.ALWAYS);
                columnConstraints2.setMinWidth(300);
                gridPane.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);
                AnchorPane.setBottomAnchor(gridPane, 20d);
                AnchorPane.setRightAnchor(gridPane, -10d);
                AnchorPane.setLeftAnchor(gridPane, 10d);
                AnchorPane.setTopAnchor(gridPane, -20d);
                bottomAnchorPane.getChildren().add(gridPane);

                CompensationRequestDisplay = new CompensationRequestDisplay(gridPane);
            }
            CompensationRequestDisplay.removeAllFields();
            CompensationRequestDisplay.createAllFields("Selected compensation request", Layout.GROUP_DISTANCE);

            //TODO
            compensationRequest.setInVotePeriod(true);

            if (compensationRequest.isWaitingForVotingPeriod()) {
                addLabel(gridPane, CompensationRequestDisplay.incrementAndGetGridRow(), "This compensation request is not open anymore for funding. Please wait until the next funding period starts.");
            } else if (compensationRequest.isInVotePeriod()) {
                voteButton = addButtonAfterGroup(gridPane, CompensationRequestDisplay.incrementAndGetGridRow(), "Vote on compensation request");
                voteButton.setOnAction(event -> {
                    compensationRequestManger.setSelectedCompensationRequest(compensationRequest);
                    navigation.navigateTo(MainView.class, DaoView.class, VotingView.class, VoteView.class);
                });
            } else if (compensationRequest.isInFundingPeriod()) {
                checkArgument(compensationRequest.isAccepted(), "A compensation request with state OPEN_FOR_FUNDING must be accepted.");
                fundButton = addButtonAfterGroup(gridPane, CompensationRequestDisplay.incrementAndGetGridRow(), "Fund compensation request");
                fundButton.setOnAction(event -> {
                    fundCompensationRequestWindow.applyCompensationRequest(compensationRequest.getCompensationRequestPayload()).
                            onAction(() -> {
                                Coin amount = btcFormatter.parseToCoin(fundCompensationRequestWindow.getAmount().getText());
                                compensationRequestManger.fundCompensationRequest(compensationRequest, amount,
                                        new FutureCallback<Transaction>() {
                                            @Override
                                            public void onSuccess(Transaction transaction) {
                                                UserThread.runAfter(() -> new Popup<>().feedback("Compensation request successfully funded.").show(), 1);
                                            }

                                            @Override
                                            public void onFailure(@NotNull Throwable t) {
                                                UserThread.runAfter(() -> new Popup<>().error(t.toString()).show(), 1);

                                            }
                                        });
                            }).show();
                });
            } else if (compensationRequest.isClosed()) {
                addLabel(gridPane, CompensationRequestDisplay.incrementAndGetGridRow(), "This compensation request is not open anymore for funding. Please wait until the next funding period starts.");
            }
            CompensationRequestDisplay.setAllFieldsEditable(false);

            CompensationRequestDisplay.fillWithData(compensationRequest.getCompensationRequestPayload());
        }
    }

    private void setColumns() {
        TableColumn<CompensationRequest, CompensationRequest> dateColumn = new TableColumn<CompensationRequest, CompensationRequest>("Date/Time") {
            {
                setMinWidth(190);
                setMaxWidth(190);
            }
        };
        dateColumn.setCellValueFactory((tradeStatistics) -> new ReadOnlyObjectWrapper<>(tradeStatistics.getValue()));
        dateColumn.setCellFactory(
                new Callback<TableColumn<CompensationRequest, CompensationRequest>, TableCell<CompensationRequest,
                        CompensationRequest>>() {
                    @Override
                    public TableCell<CompensationRequest, CompensationRequest> call(
                            TableColumn<CompensationRequest, CompensationRequest> column) {
                        return new TableCell<CompensationRequest, CompensationRequest>() {
                            @Override
                            public void updateItem(final CompensationRequest item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null)
                                    setText(formatter.formatDateTime(item.getCompensationRequestPayload().creationDate));
                                else
                                    setText("");
                            }
                        };
                    }
                });
        dateColumn.setComparator((o1, o2) -> o1.getCompensationRequestPayload().creationDate.compareTo(o2.getCompensationRequestPayload().creationDate));
        tableView.getColumns().add(dateColumn);
        tableView.getSortOrder().add(dateColumn);


        TableColumn<CompensationRequest, CompensationRequest> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory((tradeStatistics) -> new ReadOnlyObjectWrapper<>(tradeStatistics.getValue()));
        nameColumn.setCellFactory(
                new Callback<TableColumn<CompensationRequest, CompensationRequest>, TableCell<CompensationRequest,
                        CompensationRequest>>() {
                    @Override
                    public TableCell<CompensationRequest, CompensationRequest> call(
                            TableColumn<CompensationRequest, CompensationRequest> column) {
                        return new TableCell<CompensationRequest, CompensationRequest>() {
                            @Override
                            public void updateItem(final CompensationRequest item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null)
                                    setText(item.getCompensationRequestPayload().name);
                                else
                                    setText("");
                            }
                        };
                    }
                });
        nameColumn.setComparator((o1, o2) -> o1.getCompensationRequestPayload().name.compareTo(o2.getCompensationRequestPayload().name));
        tableView.getColumns().add(nameColumn);
    }
}
